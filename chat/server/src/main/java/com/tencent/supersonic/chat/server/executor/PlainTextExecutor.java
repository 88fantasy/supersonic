package com.tencent.supersonic.chat.server.executor;

import com.google.common.collect.ImmutableMap;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.SseService;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.CustomAiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.DeepSeekModelFactory;
import dev.langchain4j.provider.ModelProvider;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingReasoningChatGenerator;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlainTextExecutor extends ExecutorAgent {

    public static final String NODE_NAME = "PlainTextExecutor";

    public static final String APP_KEY = "SMALL_TALK";
    private static final String INSTRUCTION = "#Role: You are a nice person to talk to."
            + "\n#Task: Respond quickly and nicely to the user."
            + "\n#Rules: 1.ALWAYS use the same language as the `#Current Input`."
            + "\n#History Inputs: %s" + "\n#Current Input: %s" + "\n#Response: ";

    public PlainTextExecutor() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("闲聊对话")
                .appModule(AppModule.CHAT).description("直接将原始输入透传大模型").enable(false)
                .providerPrompts(Map.of(DeepSeekModelFactory.PROVIDER, """
                        # 角色: 你是一位理想的对话伙伴，擅长以快速且友好的方式回应用户。
                        # 任务: 根据用户的当前需求提供及时、友好且相关的回复。
                        # 规则:
                        1. 请确保你的回答与用户提问时使用的语言风格保持一致。
                        2. 输出的内容需要遵循Markdown格式规范。
                        3. 尽量让对话自然流畅，同时考虑到上下文信息。
                        # 历史对话: %s
                        # 当前问题: %s
                        # 回应:
                        """))
                .build());
    }


    @Override
    public String name() {
        return NODE_NAME;
    }

    @Override
    public Map<String, Object> apply(ExecuteContext executeContext) throws Exception {
        if (QueryState.EMPTY.equals(executeContext.getResponse().getQueryState()) && "PLAIN_TEXT".equals(executeContext.getParseInfo().getQueryMode())) {
            ChatExecuteReq request = executeContext.getRequest();
            AgentService agentService = ContextUtils.getBean(AgentService.class);
            Agent chatAgent = agentService.getAgent(executeContext.getAgent().getId());
            ChatApp chatApp = chatAgent.getChatAppConfig().get(APP_KEY);
            if (Objects.nonNull(chatApp) && chatApp.isEnable()) {
                String promptStr = String.format(chatApp.getPrompt(), getHistoryInputs(executeContext),
                        request.getQueryText());
                Prompt prompt = PromptTemplate.from(promptStr).apply(Collections.EMPTY_MAP);
                ChatModelConfig chatModelConfig = chatApp.getChatModelConfig();

                if (request.isStream()) {
                    String clientId = request.getClientId();
                    SseService sseService = ContextUtils.getBean(SseService.class);


                    StreamingReasoningChatGenerator<ExecuteContext, AiMessage> generator = StreamingReasoningChatGenerator.<ExecuteContext, AiMessage>builder()
                            .mapResult(r -> {
                                AiMessage content = r.content();
                                ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                                        .put(ExecuteContext.MESSAGES_KEY, content.text());
                                if (content instanceof CustomAiMessage customAiMessage) {
                                    builder.put(ExecuteContext.ATTRIBUTES_KEY, customAiMessage.attributes());
                                }
                                return builder.build();
                            })
                            .startingNode(NODE_NAME)
                            .startingState(executeContext)
                            .build();

//                    final CountDownLatch countDownLatch = new CountDownLatch(1);

                    StreamingChatLanguageModel streamingChatLanguageModel = ModelProvider.getStreamingChatModel(chatModelConfig);
                    streamingChatLanguageModel.generate(prompt.toUserMessage(), generator.handler());
                    for (StreamingOutput<ExecuteContext> r : generator) {
                        QueryResult result = new QueryResult();
                        result.setQueryId(request.getQueryId());
                        result.setQueryState(QueryState.PENDING);
                        result.setQueryMode(executeContext.getParseInfo().getQueryMode());
                        result.setTextResult("");
                        result.setResponse(Map.of("reasoningContent", r.chunk()));
                        sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                    }
                    Optional<Object> optional = generator.resultValue();
                    if (optional.isEmpty()) {

                    }
                    Map<String, Object> response = (Map<String, Object>) optional.get();
                    QueryResult result = new QueryResult();
                    result.setQueryState(QueryState.SUCCESS);
                    result.setQueryMode(executeContext.getParseInfo().getQueryMode());
                    result.setTextResult((String) response.getOrDefault(ExecuteContext.MESSAGES_KEY, ""));
                    result.setResponse(response.get(ExecuteContext.ATTRIBUTES_KEY));
                    sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
                    return Map.of(ExecuteContext.RESPONSE_KEY, result);
//                    return AgentState.updateState(executeContext, response, ExecuteContext.SCHEMA);

//                    streamingChatLanguageModel.generate(prompt.toUserMessage(), new StreamingReasoningResponseHandler<>() {
//                        @Override
//                        public void onNextReasoning(String token) {
//                            if (StringUtils.hasText(token)) {
//                                QueryResult result = new QueryResult();
//                                result.setQueryId(request.getQueryId());
//                                result.setQueryState(QueryState.PENDING);
//                                result.setQueryMode(executeContext.getParseInfo().getQueryMode());
//                                result.setTextResult("");
//                                result.setResponse(Map.of("reasoningContent", token));
//                                sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
//                            }
//                        }
//
//                        @Override
//                        public void onNext(String token) {
//                            if (StringUtils.hasText(token)) {
//                                QueryResult result = new QueryResult();
//                                result.setQueryId(request.getQueryId());
//                                result.setQueryState(QueryState.PENDING);
//                                result.setQueryMode(executeContext.getParseInfo().getQueryMode());
//                                result.setTextResult(token);
//                                sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
//                            }
//                        }
//
//                        @Override
//                        public void onError(Throwable error) {
//                            try {
//                                QueryResult result = new QueryResult();
//                                result.setQueryState(QueryState.INVALID);
//                                result.setQueryMode(executeContext.getParseInfo().getQueryMode());
//                                result.setErrorMsg(error.getMessage());
//                                executeContext.setResponse(result);
//                                sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
//
//                            } finally {
//                                countDownLatch.countDown();
//                            }
//                        }
//
//                        @Override
//                        public void onComplete(Response<AiMessage> response) {
//                            try {
//                                QueryResult result = new QueryResult();
//                                result.setQueryState(QueryState.SUCCESS);
//                                result.setQueryMode(executeContext.getParseInfo().getQueryMode());
//                                result.setTextResult(response.content().text());
//                                if (response.content() instanceof CustomAiMessage customAiMessage) {
//                                    result.setResponse(customAiMessage.attributes());
//                                }
//                                executeContext.setResponse(result);
//                                sseService.send(clientId, SseEmitter.event().data(result, MediaType.APPLICATION_JSON).reconnectTime(3000L));
//                            } finally {
//                                countDownLatch.countDown();
//                            }
//                        }
//                    });
//                    try {
//                        countDownLatch.await(300, TimeUnit.SECONDS);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }

                } else {
                    ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(chatModelConfig);
                    Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());

                    QueryResult result = new QueryResult();
                    result.setQueryState(QueryState.SUCCESS);
                    result.setQueryMode(executeContext.getParseInfo().getQueryMode());
                    result.setTextResult(response.content().text());
                    if (response.content() instanceof CustomAiMessage customAiMessage) {
                        result.setResponse(customAiMessage.attributes());
                    }
                    return Map.of(ExecuteContext.RESPONSE_KEY, result);
                }
            }
        }
        return Map.of();
    }

    private String getHistoryInputs(ExecuteContext executeContext) {
        StringBuilder historyInput = new StringBuilder();
        List<QueryResp> queryResps = getHistoryQueries(executeContext.getRequest().getChatId(), 5);
        queryResps.forEach(p -> {
            historyInput.append(p.getQueryText());
            historyInput.append(";");

        });

        return historyInput.toString();
    }

    private List<QueryResp> getHistoryQueries(int chatId, int multiNum) {
        ChatManageService chatManageService = ContextUtils.getBean(ChatManageService.class);
        List<QueryResp> contextualParseInfoList = chatManageService.getChatQueries(chatId).stream()
                .filter(q -> Objects.nonNull(q.getQueryResult())
                        && q.getQueryResult().getQueryState() == QueryState.SUCCESS)
                .collect(Collectors.toList());

        List<QueryResp> contextualList = contextualParseInfoList.subList(0,
                Math.min(multiNum, contextualParseInfoList.size()));
        Collections.reverse(contextualList);

        return contextualList;
    }

}
