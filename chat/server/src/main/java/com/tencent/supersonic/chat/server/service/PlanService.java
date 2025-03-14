package com.tencent.supersonic.chat.server.service;


import com.alibaba.fastjson2.JSON;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.server.parser.ChatQueryParser;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.pojo.SemanticParse;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanService {

    public static final String APP_KEY_INTENT_MAPPING = "INTENT_MAPPING";

    public PlanService() {
        ChatAppManager.register(APP_KEY_INTENT_MAPPING, ChatApp.builder()
                .prompt("""
                        你是一个专业的意图分类器，需要根据用户输入准确判断意图类型，严格按照以下规则输出JSON：
                        
                        # 意图分类规则
                        {{parsers}}
                        
                        # 输出要求
                        - 只使用指定意图标签：{{parserNames}}
                        - 禁用任何解释说明
                        - 如果不属于上述任何意图，请返回未知，并根据用户输入问题进行意图分类规则内的思考并给出至少三个推荐的问题，严格按照以下格式：{"intention":"unknown","recommends":["<question1>","<question2>","<question3>"]}
                        - 必须输出标准JSON格式：{"intention":"<意图标签>", "keywords": [<关键信息>]}
                        
                        # 示例对照
                        {{fetches}}
                        
                        """).name("意图识别").appModule(AppModule.CHAT)
                .description("通过大模型识别用户的使用场景并提取关键信息").enable(false)
                .build());
    }

    public void mapping(ParseContext parseContext, List<ChatQueryParser> parsers) {
        ChatParseResp chatParseResp = parseContext.getResponse();
        ChatApp chatApp = parseContext.getAgent().getChatAppConfig().get(APP_KEY_INTENT_MAPPING);
        if (Objects.nonNull(chatApp) && chatApp.isEnable()) {

            StringBuilder parsersStr = new StringBuilder();
            for(int i = 0, j = i + 1; i< parsers.size(); i++) {
                ChatQueryParser chatQueryParser = parsers.get(i);
                parsersStr.append(String.format("%s. %s(%s):\n",j, chatQueryParser.name(), chatQueryParser.code()));
                parsersStr.append(String.format("- %s\n",chatQueryParser.description()));
                parsersStr.append(String.format("- %s\n", String.join(",", chatQueryParser.keywords())));
                parsersStr.append(String.format("- %s\n", String.join(" / ", chatQueryParser.exemplars())));
            }


            Map<String,Object> variables = new ConcurrentHashMap<>();
            variables.put("parsers", parsersStr.toString());
            variables.put("parserNames", parsers.stream().map(ChatQueryParser::code).collect(Collectors.joining("/")));

            Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variables);
            ChatModelConfig chatModelConfig = chatApp.getChatModelConfig();

            ChatParseReq request = parseContext.getRequest();
            if (request.isStream()) {
                String clientId = request.buildClientId();
                SseService sseService = ContextUtils.getBean(SseService.class);

//                final CountDownLatch countDownLatch = new CountDownLatch(1);
                //            StreamingChatLanguageModel streamingChatLanguageModel = ModelProvider.getStreamingChatModel(chatModelConfig);
                //            streamingChatLanguageModel.generate(prompt.toUserMessage(), new StreamingReasoningResponseHandler<>() {
                //                @Override
                //                public void onNextReasoning(String token) {
                //                    if (StringUtils.hasText(token)) {
                //                        QueryResult result = new QueryResult();
                //                        result.setQueryId(request.getQueryId());
                //                        result.setQueryState(QueryState.PENDING);
                //                        result.setQueryMode(executeContext.getParseInfo().getQueryMode());
                //                        result.setTextResult("");
                //                        result.setResponse(Map.of("reasoningContent", token));
                //                        sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                //                    }
                //                }
                //
                //                @Override
                //                public void onNext(String token) {
                //                    if (StringUtils.hasText(token)) {
                //                        QueryResult result = new QueryResult();
                //                        result.setQueryId(request.getQueryId());
                //                        result.setQueryState(QueryState.PENDING);
                //                        result.setQueryMode(executeContext.getParseInfo().getQueryMode());
                //                        result.setTextResult(token);
                //                        sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                //                    }
                //                }
                //
                //                @Override
                //                public void onError(Throwable error) {
                //                    try {
                //                        QueryResult result = new QueryResult();
                //                        result.setQueryState(QueryState.INVALID);
                //                        result.setQueryMode(executeContext.getParseInfo().getQueryMode());
                //                        result.setErrorMsg(error.getMessage());
                //                        executeContext.setResponse(result);
                //                        sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                //
                //                    } finally {
                //                        countDownLatch.countDown();
                //                    }
                //                }
                //
                //                @Override
                //                public void onComplete(Response<AiMessage> response) {
                //                    try {
                //                        QueryResult result = new QueryResult();
                //                        result.setQueryState(QueryState.SUCCESS);
                //                        result.setQueryMode(executeContext.getParseInfo().getQueryMode());
                //                        result.setTextResult(response.content().text());
                //                        if (response.content() instanceof CustomAiMessage customAiMessage) {
                //                            result.setResponse(customAiMessage.attributes());
                //                        }
                //                        executeContext.setResponse(result);
                //                        sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                //                    } finally {
                //                        countDownLatch.countDown();
                //                    }
                //                }
                //            });
                //            try {
                //                countDownLatch.await(300, TimeUnit.SECONDS);
                //            } catch (InterruptedException e) {
                //                throw new RuntimeException(e);
                //            }

            } else {
                ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(chatModelConfig);


                Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage(),
                        new UserMessage(request.getQueryText()));
                String text = response.content().text();
                SemanticParse semanticParse = JSON.parseObject(text, SemanticParse.class);

//                chatParseResp.getSelectedParses().add()

            }
        }
    }
}
