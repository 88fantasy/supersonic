package dev.langchain4j.provider;

import java.util.Map;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import com.tencent.supersonic.common.util.AESEncryptionUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.deepseek.DeepSeekChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeepSeekModelFactory implements ModelFactory {
    public static final String PROVIDER = "DEEPSEEK";

    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    public static final String DEFAULT_MODEL_NAME = "deepseek-chat";

    private static final List<Parameter> PARAMETERS = List.of(
            new Parameter("baseUrl", DEFAULT_BASE_URL, "BaseUrl", "",
                    "string"),
            new Parameter("apiKey", "", "ApiKey", "",
                    "password"),
            new Parameter("modelName", DEFAULT_MODEL_NAME, "ModelName",
                    "", "string"),
            new Parameter("temperature", "0.0", "Temperature", "", "slider")
    );

    private static final Map<String, String> PROMPTS = Map.of(
//            ErrorMsgRewriteProcessor.
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
    public boolean supportEmbedding() {
        return false;
    }

    @Override
    public String prompt(String key) {
        return ModelFactory.super.prompt(key);
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return DeepSeekChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(AESEncryptionUtil.aesDecryptECB(modelConfig.getApiKey()))
                .modelName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature())
                .timeout(modelConfig.getTimeOut()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return null;
    }

}
