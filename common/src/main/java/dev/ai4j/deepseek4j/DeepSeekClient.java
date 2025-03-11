package dev.ai4j.deepseek4j;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.ai4j.deepseek4j.chat.ChatCompletionRequest;
import dev.ai4j.deepseek4j.chat.ChatCompletionResponse;
import dev.ai4j.deepseek4j.models.ModelsResponse;
import dev.ai4j.openai4j.OpenAiClient;
import dev.langchain4j.model.qianfan.client.AuthorizationHeaderInjector;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static dev.langchain4j.internal.Utils.getOrDefault;

@Slf4j
public class DeepSeekClient {

    private final String baseUrl;

    private final String model;

    private final OkHttpClient okHttpClient;

    private final DeepSeekApi deepSeekApi;

    private final boolean logStreamingResponses;


    @lombok.Builder
    public DeepSeekClient(String baseUrl, String modelName, String apiKey, Duration callTimeout,
            Duration connectTimeout, Duration readTimeout, Duration writeTimeout,
            boolean logRequests, boolean logResponses, LogLevel logLevel,
            boolean logStreamingResponses, Map<String, String> customHeaders) {
        this.baseUrl = getOrDefault(baseUrl, "https://api.deepseek.com/");
        this.model = modelName;

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .callTimeout(getOrDefault(callTimeout, Duration.ofSeconds(60)))
                .connectTimeout(getOrDefault(connectTimeout, Duration.ofSeconds(60)))
                .readTimeout(getOrDefault(readTimeout, Duration.ofSeconds(60)))
                .writeTimeout(getOrDefault(writeTimeout, Duration.ofSeconds(60)));

        if (apiKey == null) {
            throw new IllegalArgumentException("apiKey must be defined");
        }
        okHttpClientBuilder.addInterceptor(new AuthorizationHeaderInjector(apiKey));

        Map<String, String> headers = new HashMap<>();
        if (customHeaders != null) {
            headers.putAll(customHeaders);
        }
        if (!headers.isEmpty()) {
            okHttpClientBuilder.addInterceptor(new GenericHeaderInjector(headers));
        }

        if (logRequests) {
            okHttpClientBuilder.addInterceptor(
                    new RequestLoggingInterceptor(getOrDefault(logLevel, LogLevel.DEBUG)));
        }

        if (logResponses) {
            okHttpClientBuilder.addInterceptor(
                    new ResponseLoggingInterceptor(getOrDefault(logLevel, LogLevel.DEBUG)));
        }
        this.logStreamingResponses = logStreamingResponses;

        this.okHttpClient = okHttpClientBuilder.build();

        Retrofit.Builder retrofitBuilder =
                new Retrofit.Builder().baseUrl(baseUrl).client(okHttpClient);

        retrofitBuilder.addConverterFactory(JacksonConverterFactory
                .create(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)));

        this.deepSeekApi = retrofitBuilder.build().create(DeepSeekApi.class);

    }

    public void shutdown() {
        okHttpClient.dispatcher().executorService().shutdown();

        okHttpClient.connectionPool().evictAll();

        Cache cache = okHttpClient.cache();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                log.error("Failed to close cache", e);
            }
        }
    }

    public SyncOrAsyncOrStreaming<ChatCompletionResponse> chatCompletion(
            ChatCompletionRequest request) {
        return chatCompletion(new DeepSeekClientContext(), request);
    }


    public SyncOrAsyncOrStreaming<ChatCompletionResponse> chatCompletion(
            DeepSeekClientContext context, ChatCompletionRequest request) {

        if (Objects.isNull(request.getModel())) {
            request.setModel(this.model);
        }

        ChatCompletionRequest syncRequest =
                ChatCompletionRequest.builder().from(request).stream(false).build();

        return new RequestExecutor<>(deepSeekApi.chatCompletions(context.headers(), syncRequest),
                r -> r, okHttpClient, formatUrl("chat/completions"),
                () -> ChatCompletionRequest.builder().from(request).stream(true).build(),
                ChatCompletionResponse.class, r -> r, logStreamingResponses);
    }


    public SyncOrAsyncOrStreaming<String> chatCompletion(DeepSeekClientContext context,
            String userMessage) {
        ChatCompletionRequest request =
                ChatCompletionRequest.builder().addUserMessage(userMessage).build();

        ChatCompletionRequest syncRequest =
                ChatCompletionRequest.builder().from(request).stream(false).build();

        return new RequestExecutor<>(deepSeekApi.chatCompletions(context.headers(), syncRequest),
                ChatCompletionResponse::content, okHttpClient, formatUrl("chat/completions"),
                () -> ChatCompletionRequest.builder().from(request).stream(true).build(),
                ChatCompletionResponse.class, r -> r.choices().get(0).delta().content(),
                logStreamingResponses);
    }

    public ModelsResponse models() {

        return new RequestExecutor<>(this.deepSeekApi.models(new HashMap<>()), r -> r, okHttpClient,
                null, null, ModelsResponse.class, null, logStreamingResponses).execute();
    }

    private String formatUrl(String endpoint) {
        return baseUrl + endpoint;
    }

    public static class DeepSeekClientContext {

        private final Map<String, String> headers = new HashMap<>();

        public DeepSeekClientContext addHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public DeepSeekClientContext addHeader(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public static DeepSeekClientContext create() {
            return new DeepSeekClientContext();
        }

    }

}
