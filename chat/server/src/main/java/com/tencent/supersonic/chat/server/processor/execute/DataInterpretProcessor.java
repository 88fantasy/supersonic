package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.SseService;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.CustomAiMessage;
import dev.langchain4j.model.StreamingReasoningResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.DeepSeekModelFactory;
import dev.langchain4j.provider.ModelProvider;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * DataInterpretProcessor interprets query result to make it more readable to the users.
 */
@Service
public class DataInterpretProcessor implements ExecuteResultProcessor, NodeAction<ExecuteContext> {

    public static final String NODE_NAME = "DataInterpretProcessor";

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY = "DATA_INTERPRETER";
    private static final String INSTRUCTION = ""
            + "#Role: You are a data expert who communicates with business users everyday."
            + "\n#Task: Your will be provided with a question asked by a user and the relevant "
            + "result data queried from the databases, please interpret the data and organize a brief answer."
            + "\n#Rules: " + "\n1.ALWAYS respond in the use the same language as the `#Question`."
            + "\n2.ALWAYS reference some key data in the `#Answer`."
            + "\n#Question:{{question}} #Data:{{data}} #Answer:";

    public DataInterpretProcessor() {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("结果数据解读").appModule(AppModule.CHAT)
                        .description("通过大模型对结果数据做提炼总结").enable(false)
                        .providerPrompts(Map.of(DeepSeekModelFactory.PROVIDER, """
                                #角色: 你是一名每天与业务用户沟通的数据专家。
                                #任务: 你将收到用户提出的问题以及从数据库中查询到的相关结果数据，请解读数据并组织简短回答。
                                #规则:
                                1.始终使用和`问题`相同的语言进行解读。
                                2.始终在`回答`中引用一些关键数据。
                                #问题:{{question}}
                                #数据:{{data}}
                                #回答:
                                """)).build());
    }

    @Override
    public Map<String, Object> apply(ExecuteContext executeContext) throws Exception {
        if (accept(executeContext)) {
            process(executeContext);
        }
        return Map.of();
    }


    @Override
    public boolean accept(ExecuteContext executeContext) {
        Agent agent = executeContext.getAgent();
        ChatApp chatApp = agent.getChatAppConfig().get(APP_KEY);
        return Objects.nonNull(chatApp) && chatApp.isEnable();
    }

    @Override
    public void process(ExecuteContext executeContext) {
        ChatExecuteReq request = executeContext.getRequest();
        QueryResult queryResult = executeContext.getResponse();
        Agent agent = executeContext.getAgent();
        ChatApp chatApp = agent.getChatAppConfig().get(APP_KEY);
        ChatModelConfig chatModelConfig = chatApp.getChatModelConfig();

        Map<String, Object> variable = new HashMap<>();
        variable.put("question", executeContext.getRequest().getQueryText());
        variable.put("data", queryResult.getTextResult());

        Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variable);

        if (request.isStream()) {
            String clientId = request.getClientId();
            SseService sseService = ContextUtils.getBean(SseService.class);

            final CountDownLatch countDownLatch = new CountDownLatch(1);

            StreamingChatLanguageModel streamingChatLanguageModel = ModelProvider.getStreamingChatModel(chatModelConfig);
            streamingChatLanguageModel.generate(prompt.toUserMessage(), new StreamingReasoningResponseHandler<>() {
                @Override
                public void onNextReasoning(String token) {
                    if (StringUtils.hasText(token)) {
                        queryResult.setQueryState(QueryState.PENDING);
                        queryResult.setTextSummary("");
                        queryResult.setResponse(Map.of("reasoningContent", token));
                        sseService.send(clientId, SseEmitter.event().data(queryResult, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                    }
                }

                @Override
                public void onNext(String token) {
                    if (StringUtils.hasText(token)) {
                        queryResult.setQueryState(QueryState.PENDING);
                        queryResult.setTextSummary(token);
                        sseService.send(clientId, SseEmitter.event().data(queryResult, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                    }
                }

                @Override
                public void onError(Throwable error) {
                    try {
                        queryResult.setQueryState(QueryState.INVALID);
                        queryResult.setErrorMsg(error.getMessage());
//                        executeContext.setResponse(queryResult);
                        sseService.send(clientId, SseEmitter.event().data(queryResult, MediaType.APPLICATION_JSON).reconnectTime(3000L));

                    } finally {
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    try {
                        queryResult.setTextSummary(response.content().text());
                        if (response.content() instanceof CustomAiMessage customAiMessage) {
                            queryResult.setResponse(customAiMessage.attributes());
                        }
//                        executeContext.setResponse(queryResult);
                        sseService.send(clientId, SseEmitter.event().data(queryResult, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });
            try {
                countDownLatch.await(300, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(chatApp.getChatModelConfig());
            Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
            String answer = response.content().text();
            keyPipelineLog.info("DataInterpretProcessor modelReq:\n{} \nmodelResp:\n{}", prompt.text(),
                    answer);
            if (StringUtils.hasText(answer)) {
                queryResult.setTextSummary(answer);
            }
        }
    }
}
