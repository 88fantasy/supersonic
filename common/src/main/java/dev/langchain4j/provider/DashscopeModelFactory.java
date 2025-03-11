package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenModelName;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashscopeModelFactory implements ModelFactory {
    public static final String PROVIDER = "DASHSCOPE";
    public static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    public static final String DEFAULT_MODEL_NAME = QwenModelName.QWEN_PLUS;
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-v2";

    private static final List<Parameter> PARAMETERS =
            List.of(new Parameter("baseUrl", DEFAULT_BASE_URL, "BaseUrl", "", "string"),
                    new Parameter("apiKey", "", "ApiKey", "", "password"),
                    new Parameter("modelName", DEFAULT_MODEL_NAME, "ModelName", "", "string"),
                    new Parameter("enableSearch", "false", "是否启用搜索增强功能，设为false表示不启用", "", "bool"),
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
        return QwenStreamingChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey()).modelName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature() == null ? 0L
                        : modelConfig.getTemperature().floatValue())
                .topP(modelConfig.getTopP()).enableSearch(modelConfig.getEnableSearch()).build();
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return QwenChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey()).modelName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature() == null ? 0L
                        : modelConfig.getTemperature().floatValue())
                .topP(modelConfig.getTopP()).enableSearch(modelConfig.getEnableSearch()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return QwenEmbeddingModel.builder().apiKey(embeddingModelConfig.getApiKey())
                .modelName(embeddingModelConfig.getModelName()).build();
    }

}
