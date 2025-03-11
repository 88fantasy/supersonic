package dev.ai4j.deepseek4j;

import java.util.function.Consumer;

public interface SyncOrAsyncOrStreaming<ResponseContent> extends SyncOrAsync<ResponseContent> {

    StreamingResponseHandling onPartialResponse(Consumer<ResponseContent> partialResponseHandler);

}
