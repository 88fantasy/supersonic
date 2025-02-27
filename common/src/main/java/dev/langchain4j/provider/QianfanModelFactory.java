package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import dev.langchain4j.model.qianfan.QianfanEmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QianfanModelFactory implements ModelFactory {

    public static final String PROVIDER = "QIANFAN";
    public static final String DEFAULT_BASE_URL = "https://aip.baidubce.com";
    public static final String DEFAULT_MODEL_NAME = "Llama-2-70b-chat";

    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "Embedding-V1";
    public static final String DEFAULT_ENDPOINT = "llama_2_70b";

    private static final List<Parameter> PARAMETERS = List.of(
            new Parameter("baseUrl", DEFAULT_BASE_URL, "BaseUrl", "",
                    "string"),
            new Parameter("endpoint", DEFAULT_ENDPOINT, "Endpoint", "",
                    "string"),
            new Parameter("apiKey", "", "ApiKey", "",
                    "password"),
            new Parameter("secretKey", "demo",
                    "SecretKey", "", "password"),
            new Parameter("modelName", DEFAULT_MODEL_NAME, "ModelName",
                    "", "string"),
            new Parameter("temperature", "0.0", "Temperature", "", "slider"),
            new Parameter("timeOut", "60", "超时时间(秒)", "", "number")
    );

    @Override
    public String type() {
        return PROVIDER;
    }

    @Override
    public List<Parameter> chatParameters() {
        return PARAMETERS;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return QianfanChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey()).secretKey(modelConfig.getSecretKey())
                .endpoint(modelConfig.getEndpoint()).modelName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature()).topP(modelConfig.getTopP())
                .maxRetries(modelConfig.getMaxRetries()).logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return QianfanEmbeddingModel.builder().baseUrl(embeddingModelConfig.getBaseUrl())
                .apiKey(embeddingModelConfig.getApiKey())
                .secretKey(embeddingModelConfig.getSecretKey())
                .modelName(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses()).build();
    }

}
