package org.bsc.langgraph4j.state;


import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class MapChannel extends BaseChannel<Map<String,Object>>{

    public MapChannel(boolean replace) {

        super((oldValue, newValue) -> {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                    .putAll(oldValue);
            if(replace) {
                builder.putAll(newValue);
            }
            else {
                for(String key : newValue.keySet()) {
                    if(!oldValue.containsKey(key)) {
                        oldValue.put(key, newValue.get(key));
                    }
                }
            }
            return  builder.build();
        }, Map::of);
    }
}
