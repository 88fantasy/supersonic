package dev.langchain4j.provider;

import com.tencent.supersonic.common.config.EmbeddingModelParameterConfig;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelProvider {

    public static final ChatModelConfig DEMO_CHAT_MODEL =
            ChatModelConfig.builder().provider("OPEN_AI").baseUrl("https://api.openai.com/v1")
                    .apiKey("demo").modelName("gpt-4o-mini").temperature(0.0).timeOut(60L).build();

    private static final Map<String, ModelFactory> factories = new HashMap<>();

    public static void add(String provider, ModelFactory modelFactory) {
        factories.put(provider, modelFactory);
    }

    public static Collection<ModelFactory> factories() {
        return factories.values();
    }

    public static ModelFactory getFactory(String model) {
        if (!factories.containsKey(model)) {
            throw new RuntimeException("Unknown Model Provider: " + model);
        }
        return factories.get(model);
    }

    public static StreamingChatLanguageModel getStreamingChatModel() {
        return getStreamingChatModel(null);
    }

    public static StreamingChatLanguageModel getStreamingChatModel(ChatModelConfig modelConfig) {
        if (modelConfig == null || StringUtils.isBlank(modelConfig.getProvider())
                || StringUtils.isBlank(modelConfig.getBaseUrl())) {
            modelConfig = DEMO_CHAT_MODEL;
        }
        ModelFactory modelFactory = factories.get(modelConfig.getProvider().toUpperCase());
        if (modelFactory != null) {
            return modelFactory.createStreamingChatModel(modelConfig);
        }

        throw new RuntimeException(
                "Unsupported StreamingChatLanguageModel provider: " + modelConfig.getProvider());
    }

    public static ChatLanguageModel getChatModel() {
        return getChatModel(null);
    }

    public static ChatLanguageModel getChatModel(ChatModelConfig modelConfig) {
        if (modelConfig == null || StringUtils.isBlank(modelConfig.getProvider())
                || StringUtils.isBlank(modelConfig.getBaseUrl())) {
            modelConfig = DEMO_CHAT_MODEL;
        }
        ModelFactory modelFactory = factories.get(modelConfig.getProvider().toUpperCase());
        if (modelFactory != null) {
            return modelFactory.createChatModel(modelConfig);
        }

        throw new RuntimeException(
                "Unsupported ChatLanguageModel provider: " + modelConfig.getProvider());
    }

    public static EmbeddingModel getEmbeddingModel() {
        return getEmbeddingModel(null);
    }

    public static EmbeddingModel getEmbeddingModel(EmbeddingModelConfig embeddingModel) {
        if (embeddingModel == null || StringUtils.isBlank(embeddingModel.getProvider())) {
            EmbeddingModelParameterConfig parameterConfig =
                    ContextUtils.getBean(EmbeddingModelParameterConfig.class);
            embeddingModel = parameterConfig.convert();
        }
        String provider = embeddingModel.getProvider();
        if (!factories.containsKey(provider)) {
            throw new RuntimeException("Unknown EmbeddingModel provider: " + provider);
        }
        ModelFactory modelFactory = factories.get(provider);
        if (!modelFactory.supportEmbedding()) {
            throw new RuntimeException("Unsupported EmbeddingModel provider: " + provider);
        }
        return modelFactory.createEmbeddingModel(embeddingModel);
    }
}
