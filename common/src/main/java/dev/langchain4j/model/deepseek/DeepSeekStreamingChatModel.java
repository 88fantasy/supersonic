package dev.langchain4j.model.deepseek;


import com.tencent.supersonic.common.util.StringUtil;
import dev.ai4j.deepseek4j.DeepSeekClient;
import dev.ai4j.deepseek4j.chat.ChatCompletionChoice;
import dev.ai4j.deepseek4j.chat.ChatCompletionRequest;
import dev.ai4j.deepseek4j.chat.ChatCompletionResponse;
import dev.ai4j.deepseek4j.chat.Delta;
import dev.ai4j.deepseek4j.chat.ResponseFormat;
import dev.ai4j.deepseek4j.chat.ResponseFormatType;
import dev.ai4j.deepseek4j.shared.StreamOptions;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingReasoningResponseHandler;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.Response;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNullOrBlank;
import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.toDeepSeekMessages;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Slf4j
public class DeepSeekStreamingChatModel implements StreamingChatLanguageModel {

    public static final String DEFAULT_URL = "https://api.deepseek.com";

    private final DeepSeekClient client;
    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final List<String> stop;
    private final Integer maxTokens;
    private final Double presencePenalty;
    private final Double frequencyPenalty;
    private final ResponseFormat responseFormat;
    private final Boolean strictTools;
    private final List<ChatModelListener> listeners;

    @Builder
    public DeepSeekStreamingChatModel(String baseUrl, String apiKey, String modelName, Double temperature,
                                      Double topP, List<String> stop, Integer maxTokens, Double presencePenalty,
                                      Double frequencyPenalty, String responseFormat,
                                      Boolean strictTools, Long timeout, Boolean logRequests,
                                      Boolean logResponses, Map<String, String> customHeaders,
                                      List<ChatModelListener> listeners) {
        baseUrl = getOrDefault(baseUrl, DEFAULT_URL);

        timeout = getOrDefault(timeout, 60L);

        logRequests = getOrDefault(logRequests, false);

        logResponses = getOrDefault(logResponses, false);

        this.client = DeepSeekClient.builder().apiKey(apiKey).baseUrl(baseUrl)
                .callTimeout(Duration.ofSeconds(timeout))
                .connectTimeout(Duration.ofSeconds(timeout))
                .readTimeout(Duration.ofSeconds(timeout)).writeTimeout(Duration.ofSeconds(timeout))
                .logRequests(logRequests).logResponses(logResponses).customHeaders(customHeaders)
                .build();
        this.modelName = getOrDefault(modelName, DeepSeekModelName.DEEPSEEK_CHAT.modelName());
        this.temperature = getOrDefault(temperature, 0.7);
        this.topP = topP;
        this.stop = stop;
        this.maxTokens = maxTokens;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
        this.responseFormat = responseFormat == null ? null
                : ResponseFormat.builder()
                        .type(ResponseFormatType.valueOf(responseFormat.toUpperCase(Locale.ROOT)))
                        .build();
        this.strictTools = getOrDefault(strictTools, false);
        this.listeners = listeners == null ? emptyList() : new ArrayList<>(listeners);
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        generate(messages, null, null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) {
        generate(messages, toolSpecifications, null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, ToolSpecification toolSpecification, StreamingResponseHandler<AiMessage> handler) {
        generate(messages, null, toolSpecification, handler);
    }

    private void generate(List<ChatMessage> messages,
                          List<ToolSpecification> toolSpecifications,
                          ToolSpecification toolThatMustBeExecuted,
                          StreamingResponseHandler<AiMessage> handler
    ) {
        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                .stream(true)
                .streamOptions(StreamOptions.builder()
                        .includeUsage(true)
                        .build())
                .model(modelName)
                .messages(toDeepSeekMessages(messages))
                .temperature(temperature)
                .topP(topP)
                .stop(stop)
                .maxTokens(maxTokens)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty)
                .responseFormat(responseFormat);

        if (toolThatMustBeExecuted != null) {
            requestBuilder.tools(InternalDeepSeekHelper.toTools(singletonList(toolThatMustBeExecuted), strictTools));
            requestBuilder.toolChoice(toolThatMustBeExecuted.name());
        } else if (!isNullOrEmpty(toolSpecifications)) {
            requestBuilder.tools(InternalDeepSeekHelper.toTools(toolSpecifications, strictTools));
        }

        ChatCompletionRequest request = requestBuilder.build();

        ChatModelRequest modelListenerRequest = InternalDeepSeekHelper.createModelListenerRequest(request, messages, toolSpecifications);
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(modelListenerRequest, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                log.warn("Exception while calling model listener", e);
            }
        });

        DeepSeekStreamingResponseBuilder responseBuilder = new DeepSeekStreamingResponseBuilder();

        AtomicReference<String> responseId = new AtomicReference<>();
        AtomicReference<String> responseModel = new AtomicReference<>();

        client.chatCompletion(request)
                .onPartialResponse(partialResponse -> {
                    responseBuilder.append(partialResponse);
                    handle(partialResponse, handler);

                    if (!isNullOrBlank(partialResponse.id())) {
                        responseId.set(partialResponse.id());
                    }
                    if (!isNullOrBlank(partialResponse.model())) {
                        responseModel.set(partialResponse.model());
                    }
                })
                .onComplete(() -> {
                    Response<AiMessage> response = responseBuilder.build();

                    ChatModelResponse modelListenerResponse = InternalDeepSeekHelper.createModelListenerResponse(
                            responseId.get(),
                            responseModel.get(),
                            response
                    );
                    ChatModelResponseContext responseContext = new ChatModelResponseContext(
                            modelListenerResponse,
                            modelListenerRequest,
                            attributes
                    );
                    listeners.forEach(listener -> {
                        try {
                            listener.onResponse(responseContext);
                        } catch (Exception e) {
                            log.warn("Exception while calling model listener", e);
                        }
                    });

                    handler.onComplete(response);
                })
                .onError(error -> {
                    Response<AiMessage> response = responseBuilder.build();

                    ChatModelResponse modelListenerPartialResponse = InternalDeepSeekHelper.createModelListenerResponse(
                            responseId.get(),
                            responseModel.get(),
                            response
                    );

                    ChatModelErrorContext errorContext = new ChatModelErrorContext(
                            error,
                            modelListenerRequest,
                            modelListenerPartialResponse,
                            attributes
                    );

                    listeners.forEach(listener -> {
                        try {
                            listener.onError(errorContext);
                        } catch (Exception e) {
                            log.warn("Exception while calling model listener", e);
                        }
                    });

                    handler.onError(error);
                })
                .execute();
    }

    private static void handle(ChatCompletionResponse partialResponse,
                               StreamingResponseHandler<AiMessage> handler) {
        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }
        Delta delta = choices.get(0).delta();
        String reasoningContent = delta.reasoningContent();
        if(handler instanceof StreamingReasoningResponseHandler<AiMessage> reasoningResponseHandler && StringUtils.hasText(reasoningContent)) {
            reasoningResponseHandler.onNextReasoning(reasoningContent);
        }
        String content = delta.content();
        if (content != null) {
            handler.onNext(content);
        }
    }
}
