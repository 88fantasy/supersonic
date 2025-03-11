package dev.ai4j.deepseek4j;

import java.util.function.Consumer;

public interface StreamingCompletionHandling {

    ErrorHandling onError(Consumer<Throwable> errorHandler);

    ErrorHandling ignoreErrors();

}
