package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class OllamaModelFactory implements ModelFactory {

    public static final String PROVIDER = "OLLAMA";
    public static final String DEFAULT_BASE_URL = "http://localhost:11434";
    public static final String DEFAULT_MODEL_NAME = "qwen:0.5b";
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "all-minilm";

    private static final List<Parameter> PARAMETERS =
            List.of(new Parameter("baseUrl", DEFAULT_BASE_URL, "BaseUrl", "", "string"),
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
        return OllamaStreamingChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .modelName(modelConfig.getModelName()).temperature(modelConfig.getTemperature())
                .timeout(Duration.ofSeconds(modelConfig.getTimeOut())).topP(modelConfig.getTopP())
                .logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses()).build();
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return OllamaChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .modelName(modelConfig.getModelName()).temperature(modelConfig.getTemperature())
                .timeout(Duration.ofSeconds(modelConfig.getTimeOut())).topP(modelConfig.getTopP())
                .maxRetries(modelConfig.getMaxRetries()).logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return OllamaEmbeddingModel.builder().baseUrl(embeddingModelConfig.getBaseUrl())
                .modelName(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses()).build();
    }

}
