package dev.ai4j.deepseek4j.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import dev.ai4j.deepseek4j.completion.Logprobs;

import java.util.List;
import java.util.Objects;

import static dev.ai4j.deepseek4j.chat.Role.ASSISTANT;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

@JsonDeserialize(builder = AssistantMessage.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class AssistantMessage implements Message {

	@JsonProperty
	private final Role role = ASSISTANT;

	@JsonProperty
	private final String content;

	@JsonProperty
	private final String reasoningContent;

	@JsonProperty
	private final String name;

	@JsonProperty
	private final List<ToolCall> toolCalls;

	@JsonProperty
	private final Logprobs logprobs;

	private AssistantMessage(Builder builder) {
		this.content = builder.content;
		this.reasoningContent = builder.reasoningContent;
		this.name = builder.name;
		this.toolCalls = builder.toolCalls;
		this.logprobs = builder.logprobs;
	}

	public Role role() {
		return role;
	}

	public String content() {
		return content;
	}

	public String name() {
		return name;
	}

	public String reasoningContent() {
		return reasoningContent;
	}

	public List<ToolCall> toolCalls() {
		return toolCalls;
	}

	public Logprobs logprobs() {
		return logprobs;
	}

	@Override
	public boolean equals(Object another) {
		if (this == another)
			return true;
		return another instanceof AssistantMessage && equalTo((AssistantMessage) another);
	}

	private boolean equalTo(AssistantMessage another) {
		return Objects.equals(role, another.role) && Objects.equals(content, another.content)
				&& Objects.equals(name, another.name) && Objects.equals(toolCalls, another.toolCalls)
				&& Objects.equals(logprobs, another.logprobs) ;
	}

	@Override
	public int hashCode() {
		int h = 5381;
		h += (h << 5) + Objects.hashCode(role);
		h += (h << 5) + Objects.hashCode(content);
		h += (h << 5) + Objects.hashCode(name);
		h += (h << 5) + Objects.hashCode(toolCalls);
		h += (h << 5) + Objects.hashCode(logprobs);
		return h;
	}

	@Override
	public String toString() {
		return "AssistantMessage{" + "role=" + role + ", content=" + content + ", name=" + name + ", toolCalls="
				+ toolCalls + ", logprobs=" + logprobs  + "}";
	}

	public static AssistantMessage from(String content) {
		return AssistantMessage.builder().content(content).build();
	}

	public static Builder builder() {
		return new Builder();
	}

	@JsonPOJOBuilder(withPrefix = "")
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static final class Builder {

		private String content;

		private String reasoningContent;

		private String name;

		private List<ToolCall> toolCalls;

		private Logprobs logprobs;

		private Builder() {
		}

		public Builder content(String content) {
			this.content = content;
			return this;
		}

		public Builder reasoningContent(String reasoningContent) {
			this.reasoningContent = reasoningContent;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder toolCalls(ToolCall... toolCalls) {
			return toolCalls(asList(toolCalls));
		}

		@JsonSetter
		public Builder toolCalls(List<ToolCall> toolCalls) {
			if (toolCalls != null) {
				this.toolCalls = unmodifiableList(toolCalls);
			}
			return this;
		}

		public Builder logprobs(Logprobs logprobs) {
			this.logprobs = logprobs;
			return this;
		}


		public AssistantMessage build() {
			return new AssistantMessage(this);
		}

	}

}
