package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class PluginExecutor implements NodeAction<ExecuteContext> {

    public static final String NODE_NAME = "PluginExecutor";

    @Override
    public Map<String, Object> apply(ExecuteContext executeContext) throws Exception {
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if (Objects.isNull(executeContext.getResponse()) && PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            PluginSemanticQuery query = PluginQueryManager.getPluginQuery(parseInfo.getQueryMode());
            query.setParseInfo(parseInfo);
            return Map.of(ExecuteContext.RESPONSE_KEY, query.build());
        }
        return Map.of();
    }
}
