package com.tencent.supersonic.common.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatApp implements Serializable {
    private String name;
    private String description;
    private String prompt;
    private Map<String, String> providerPrompts = Collections.emptyMap();
    private boolean enable;
    private Integer chatModelId;
    @JsonIgnore
    private ChatModelConfig chatModelConfig;
    @JsonIgnore
    private AppModule appModule;
}
