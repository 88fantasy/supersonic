package dev.ai4j.deepseek4j;

public interface StreamingResponseHandling extends AsyncResponseHandling {

	StreamingCompletionHandling onComplete(Runnable streamingCompletionCallback);

}
