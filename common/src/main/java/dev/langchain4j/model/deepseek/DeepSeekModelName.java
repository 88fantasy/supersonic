package dev.langchain4j.model.deepseek;


public enum DeepSeekModelName {
    DEEPSEEK_CHAT("deepseek-chat"),

    DEEPSEEK_REASONER("deepseek-reasoner");

    private final String stringValue;

    DeepSeekModelName(String stringValue) {
        this.stringValue = stringValue;
    }

    public String modelName() {
        return stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
