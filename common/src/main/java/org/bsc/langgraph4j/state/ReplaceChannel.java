package org.bsc.langgraph4j.state;


public class ReplaceChannel<T> extends BaseChannel<T> {

    public ReplaceChannel(Class<T> clazz) {
        super((oldValue, newValue) -> newValue, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
