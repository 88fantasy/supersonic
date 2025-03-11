package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import com.tencent.supersonic.common.util.AESEncryptionUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dify.DifyAiChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DifyModelFactory implements ModelFactory {
    public static final String PROVIDER = "DIFY";

    public static final String DEFAULT_BASE_URL = "https://dify.com/v1/chat-messages";
    public static final String DEFAULT_MODEL_NAME = "demo-预留-可不填写";
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "all-minilm";

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
    public boolean supportEmbedding() {
        return false;
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(ChatModelConfig modelConfig) {
        return null;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return DifyAiChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(AESEncryptionUtil.aesDecryptECB(modelConfig.getApiKey()))
                .modelName(modelConfig.getModelName()).timeOut(modelConfig.getTimeOut()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return ZhipuAiEmbeddingModel.builder().baseUrl(embeddingModelConfig.getBaseUrl())
                .apiKey(embeddingModelConfig.getApiKey()).model(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses()).build();
    }

}
