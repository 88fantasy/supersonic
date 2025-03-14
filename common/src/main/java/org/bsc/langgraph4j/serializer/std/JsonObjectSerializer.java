package org.bsc.langgraph4j.serializer.std;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class JsonObjectSerializer<T> implements NullableObjectSerializer<T> {

    private final Class<T> clazz;

    public JsonObjectSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void write(T object, ObjectOutput out) throws IOException {
        out.writeUTF(JSON.toJSONString(object));
    }

    @Override
    public T read(ObjectInput in) throws IOException, ClassNotFoundException {
        String json = in.readUTF();
        return JSON.parseObject(json, clazz);
    }
}
