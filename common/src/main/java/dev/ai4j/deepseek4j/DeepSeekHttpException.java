package dev.ai4j.deepseek4j;

public class DeepSeekHttpException extends RuntimeException {

    private final int code;

    public DeepSeekHttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }

}
