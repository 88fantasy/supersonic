package dev.langchain4j.model;


public interface StreamingReasoningResponseHandler<T> extends StreamingResponseHandler<T> {

    void onNextReasoning(String token);
}
