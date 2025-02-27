package dev.langchain4j.data.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.List;
import java.util.Map;

@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = false)
public class CustomAiMessage extends AiMessage {

    private final Map<String, Object> attributes;


    public CustomAiMessage(List<ToolExecutionRequest> toolExecutionRequests, Map<String, Object> attributes) {
        super(toolExecutionRequests);
        this.attributes = attributes;
    }

    public CustomAiMessage(String text, Map<String, Object> attributes) {
        super(text);
        this.attributes = attributes;
    }

    public CustomAiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests, Map<String, Object> attributes) {
        super(text, toolExecutionRequests);
        this.attributes = attributes;
    }

    /**
     * Returns the message attributes.
     *
     * @return the message attributes.
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    public void put(String key, Object value) {
        this.attributes.put(key, value);
    }

    public void putAll(Map<String, Object> attributes) {
        this.attributes.putAll(attributes);
    }
}