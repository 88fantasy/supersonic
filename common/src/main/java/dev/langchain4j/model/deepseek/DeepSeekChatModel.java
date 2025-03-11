package dev.langchain4j.model.deepseek;


import dev.ai4j.deepseek4j.DeepSeekClient;
import dev.ai4j.deepseek4j.DeepSeekHttpException;
import dev.ai4j.deepseek4j.chat.ChatCompletionRequest;
import dev.ai4j.deepseek4j.chat.ChatCompletionResponse;
import dev.ai4j.deepseek4j.chat.ResponseFormat;
import dev.ai4j.deepseek4j.chat.ResponseFormatType;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static dev.ai4j.deepseek4j.chat.ResponseFormatType.JSON_SCHEMA;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.aiMessageFrom;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.createModelListenerRequest;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.createModelListenerResponse;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.finishReasonFrom;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.toDeepSeekMessages;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.toDeepSeekResponseFormat;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.toTools;
import static dev.langchain4j.model.deepseek.InternalDeepSeekHelper.tokenUsageFrom;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Slf4j
public class DeepSeekChatModel implements ChatLanguageModel {

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
    private final Boolean strictJsonSchema;
    private final Boolean strictTools;
    private final Integer maxRetries;
    private final List<ChatModelListener> listeners;

    @Builder
    public DeepSeekChatModel(String baseUrl, String apiKey, String modelName, Double temperature,
            Double topP, List<String> stop, Integer maxTokens, Double presencePenalty,
            Double frequencyPenalty, String responseFormat, Boolean strictJsonSchema,
            Boolean strictTools, Long timeout, Integer maxRetries, Boolean logRequests,
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
        this.strictJsonSchema = getOrDefault(strictJsonSchema, false);
        this.strictTools = getOrDefault(strictTools, false);
        this.maxRetries = getOrDefault(maxRetries, 3);
        this.listeners = listeners == null ? emptyList() : new ArrayList<>(listeners);
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        Set<Capability> capabilities = new HashSet<>();
        if (responseFormat != null && responseFormat.type() == JSON_SCHEMA) {
            capabilities.add(RESPONSE_FORMAT_JSON_SCHEMA);
        }
        return capabilities;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return generate(messages, null, null, this.responseFormat);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications) {
        return generate(messages, toolSpecifications, null, this.responseFormat);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
            ToolSpecification toolSpecification) {
        return generate(messages, singletonList(toolSpecification), toolSpecification,
                this.responseFormat);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Response<AiMessage> response = generate(request.messages(), request.toolSpecifications(),
                null,
                getOrDefault(toDeepSeekResponseFormat(request.responseFormat(), strictJsonSchema),
                        this.responseFormat));
        return ChatResponse.builder().aiMessage(response.content())
                .tokenUsage(response.tokenUsage()).finishReason(response.finishReason()).build();
    }

    private Response<AiMessage> generate(List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications, ToolSpecification toolThatMustBeExecuted,
            ResponseFormat responseFormat) {

        if (responseFormat != null && responseFormat.type() == JSON_SCHEMA
                && responseFormat.jsonSchema() == null) {
            responseFormat = null;
        }

        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                .model(modelName).messages(toDeepSeekMessages(messages)).temperature(temperature)
                .topP(topP).stop(stop).maxTokens(maxTokens).presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty).responseFormat(responseFormat);

        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.tools(toTools(toolSpecifications, strictTools));
        }
        if (toolThatMustBeExecuted != null) {
            requestBuilder.toolChoice(toolThatMustBeExecuted.name());
        }

        ChatCompletionRequest request = requestBuilder.build();

        ChatModelRequest modelListenerRequest =
                createModelListenerRequest(request, messages, toolSpecifications);
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        ChatModelRequestContext requestContext =
                new ChatModelRequestContext(modelListenerRequest, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                log.warn("Exception while calling model listener", e);
            }
        });

        try {
            ChatCompletionResponse chatCompletionResponse =
                    withRetry(() -> client.chatCompletion(request).execute(), maxRetries);

            Response<AiMessage> response = Response.from(aiMessageFrom(chatCompletionResponse),
                    tokenUsageFrom(chatCompletionResponse.usage()),
                    finishReasonFrom(chatCompletionResponse.choices().get(0).finishReason()));

            ChatModelResponse modelListenerResponse = createModelListenerResponse(
                    chatCompletionResponse.id(), chatCompletionResponse.model(), response);
            ChatModelResponseContext responseContext = new ChatModelResponseContext(
                    modelListenerResponse, modelListenerRequest, attributes);
            listeners.forEach(listener -> {
                try {
                    listener.onResponse(responseContext);
                } catch (Exception e) {
                    log.warn("Exception while calling model listener", e);
                }
            });

            return response;
        } catch (RuntimeException e) {

            Throwable error;
            if (e.getCause() instanceof DeepSeekHttpException) {
                error = e.getCause();
            } else {
                error = e;
            }

            ChatModelErrorContext errorContext =
                    new ChatModelErrorContext(error, modelListenerRequest, null, attributes);

            listeners.forEach(listener -> {
                try {
                    listener.onError(errorContext);
                } catch (Exception e2) {
                    log.warn("Exception while calling model listener", e2);
                }
            });

            throw e;
        }
    }
}
