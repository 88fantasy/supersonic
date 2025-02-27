package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

public interface ModelFactory extends InitializingBean {

    String type();

    List<Parameter> chatParameters();

    default boolean supportChat() {
        return true;
    }

    default boolean supportEmbedding() {
        return true;
    }

    ChatLanguageModel createChatModel(ChatModelConfig modelConfig);

    EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel);

    default void afterPropertiesSet() {
        ModelProvider.add(type(), this);
    }
}
