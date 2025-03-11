package dev.ai4j.deepseek4j.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import dev.ai4j.deepseek4j.shared.StreamOptions;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

@JsonDeserialize(builder = ChatCompletionRequest.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatCompletionRequest {

    @Getter
    @Setter
    @JsonProperty
    private String model;

    @JsonProperty
    private final List<Message> messages;

    @JsonProperty
    private final Double temperature;

    @JsonProperty
    private final Double topP;

    @JsonProperty
    private final Boolean stream;

    @JsonProperty
    private final StreamOptions streamOptions;

    @JsonProperty
    private final List<String> stop;

    @JsonProperty
    private final Integer maxTokens;

    @JsonProperty
    private final Double presencePenalty;

    @JsonProperty
    private final Double frequencyPenalty;

    @JsonProperty
    private final ResponseFormat responseFormat;

    @JsonProperty
    private final List<Tool> tools;

    @JsonProperty
    private final Object toolChoice;

    @JsonProperty
    private final Boolean logprobs;

    @JsonProperty
    private final Integer topLogprobs;


    public ChatCompletionRequest(Builder builder) {
        this.model = builder.model;
        this.messages = builder.messages;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.stream = builder.stream;
        this.streamOptions = builder.streamOptions;
        this.stop = builder.stop;
        this.maxTokens = builder.maxTokens;
        this.presencePenalty = builder.presencePenalty;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.responseFormat = builder.responseFormat;
        this.tools = builder.tools;
        this.toolChoice = builder.toolChoice;
        this.logprobs = builder.logprobs;
        this.topLogprobs = builder.topLogprobs;
    }

    public String model() {
        return model;
    }

    public List<Message> messages() {
        return messages;
    }

    public Double temperature() {
        return temperature;
    }

    public Double topP() {
        return topP;
    }

    public Boolean stream() {
        return stream;
    }

    public StreamOptions streamOptions() {
        return streamOptions;
    }

    public List<String> stop() {
        return stop;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public Double presencePenalty() {
        return presencePenalty;
    }

    public Double frequencyPenalty() {
        return frequencyPenalty;
    }

    public ResponseFormat responseFormat() {
        return responseFormat;
    }

    public List<Tool> tools() {
        return tools;
    }

    public Object toolChoice() {
        return toolChoice;
    }


    private boolean equalTo(ChatCompletionRequest another) {
        return Objects.equals(model, another.model) && Objects.equals(messages, another.messages)
                && Objects.equals(temperature, another.temperature)
                && Objects.equals(topP, another.topP)
                && Objects.equals(streamOptions, another.streamOptions)
                && Objects.equals(stop, another.stop)
                && Objects.equals(presencePenalty, another.presencePenalty)
                && Objects.equals(frequencyPenalty, another.frequencyPenalty)
                && Objects.equals(tools, another.tools)
                && Objects.equals(toolChoice, another.toolChoice)
                && Objects.equals(logprobs, another.logprobs)
                && Objects.equals(topLogprobs, another.topLogprobs);
    }


    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private String model;

        private List<Message> messages;

        private Double temperature;

        private Double topP;

        private Boolean stream;

        private StreamOptions streamOptions;

        private List<String> stop;

        private Integer maxTokens;

        private Double presencePenalty;

        private Double frequencyPenalty;

        private ResponseFormat responseFormat;

        private List<Tool> tools;

        private Object toolChoice;

        private Boolean logprobs;

        private Integer topLogprobs;

        private Builder() {}

        public Builder from(ChatCompletionRequest instance) {
            model(instance.model);
            messages(instance.messages);
            temperature(instance.temperature);
            topP(instance.topP);
            stream(instance.stream);
            streamOptions(instance.streamOptions);
            stop(instance.stop);
            maxTokens(instance.maxTokens);
            presencePenalty(instance.presencePenalty);
            frequencyPenalty(instance.frequencyPenalty);
            responseFormat(instance.responseFormat);
            tools(instance.tools);
            toolChoice(instance.toolChoice);
            logprobs(instance.logprobs);
            topLogprobs(instance.topLogprobs);
            return this;
        }

        public Builder model(ChatCompletionModel model) {
            return model(model.toString());
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        @JsonSetter
        public Builder messages(List<Message> messages) {
            if (messages != null) {
                this.messages = unmodifiableList(messages);
            }
            return this;
        }

        public Builder messages(Message... messages) {
            return messages(asList(messages));
        }

        public Builder addSystemMessage(String systemMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(SystemMessage.from(systemMessage));
            return this;
        }

        public Builder addUserMessage(String userMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(UserMessage.from(userMessage));
            return this;
        }

        public Builder addAssistantMessage(String assistantMessage) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(AssistantMessage.from(assistantMessage));
            return this;
        }

        public Builder addToolMessage(String toolCallId, String content) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(ToolMessage.from(toolCallId, content));
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder streamOptions(StreamOptions streamOptions) {
            this.streamOptions = streamOptions;
            return this;
        }

        @JsonSetter
        public Builder stop(List<String> stop) {
            if (stop != null) {
                this.stop = unmodifiableList(stop);
            }
            return this;
        }

        public Builder stop(String... stop) {
            return stop(asList(stop));
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder responseFormat(ResponseFormatType responseFormatType) {
            if (responseFormatType != null) {
                responseFormat = ResponseFormat.builder().type(responseFormatType).build();
            }
            return this;
        }

        @JsonSetter
        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        @JsonSetter
        public Builder tools(List<Tool> tools) {
            if (tools != null) {
                this.tools = unmodifiableList(tools);
            }
            return this;
        }

        public Builder tools(Tool... tools) {
            return tools(asList(tools));
        }

        public Builder toolChoice(ToolChoiceMode toolChoiceMode) {
            this.toolChoice = toolChoiceMode;
            return this;
        }

        public Builder toolChoice(String functionName) {
            return toolChoice(ToolChoice.from(functionName));
        }

        public Builder toolChoice(Object toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder logprobs(Boolean logprobs) {
            this.logprobs = logprobs;
            return this;
        }

        public Builder topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return this;
        }


        public ChatCompletionRequest build() {
            return new ChatCompletionRequest(this);
        }

    }

}
