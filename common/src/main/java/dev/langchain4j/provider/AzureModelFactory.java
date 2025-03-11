package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class AzureModelFactory implements ModelFactory {
    public static final String PROVIDER = "AZURE";
    public static final String DEFAULT_BASE_URL = "https://your-resource-name.openai.azure.com/";
    public static final String DEFAULT_MODEL_NAME = "gpt-35-turbo";
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-ada-002";

    private static final List<Parameter> PARAMETERS =
            List.of(new Parameter("baseUrl", DEFAULT_BASE_URL, "BaseUrl", "", "string"),
                    new Parameter("apiKey", "", "ApiKey", "", "password"),
                    new Parameter("modelName", DEFAULT_MODEL_NAME, "ModelName", "", "string"),
                    new Parameter("temperature", "0.0", "Temperature", "", "slider"),
                    new Parameter("timeOut", "60", "超时时间(秒)", "", "number"));

    @Override
    public String type() {
        return PROVIDER;
    }

    @Override
    public List<Parameter> chatParameters() {
        return PARAMETERS;
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(ChatModelConfig modelConfig) {
        return AzureOpenAiStreamingChatModel.builder()
                .endpoint(modelConfig.getBaseUrl()).apiKey(modelConfig.getApiKey())
                .deploymentName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature()).maxRetries(modelConfig.getMaxRetries())
                .topP(modelConfig.getTopP())
                .timeout(Duration.ofSeconds(
                        modelConfig.getTimeOut() == null ? 0L : modelConfig.getTimeOut()))
                .logRequestsAndResponses(
                        modelConfig.getLogRequests() != null && modelConfig.getLogResponses())
                .build();
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .endpoint(modelConfig.getBaseUrl()).apiKey(modelConfig.getApiKey())
                .deploymentName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature()).maxRetries(modelConfig.getMaxRetries())
                .topP(modelConfig.getTopP())
                .timeout(Duration.ofSeconds(
                        modelConfig.getTimeOut() == null ? 0L : modelConfig.getTimeOut()))
                .logRequestsAndResponses(
                        modelConfig.getLogRequests() != null && modelConfig.getLogResponses());
        return builder.build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        AzureOpenAiEmbeddingModel.Builder builder =
                AzureOpenAiEmbeddingModel.builder().endpoint(embeddingModelConfig.getBaseUrl())
                        .apiKey(embeddingModelConfig.getApiKey())
                        .deploymentName(embeddingModelConfig.getModelName())
                        .maxRetries(embeddingModelConfig.getMaxRetries())
                        .logRequestsAndResponses(embeddingModelConfig.getLogRequests() != null
                                && embeddingModelConfig.getLogResponses());
        return builder.build();
    }

}
