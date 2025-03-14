package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class SupervisorExecutor extends ExecutorAgent {

    public static final String NODE_NAME = "SupervisorExecutor";

    @Override
    public String name() {
        return NODE_NAME;
    }

    @Override
    public Map<String, Object> apply(ExecuteContext executeContext) throws Exception {
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if (QueryState.SUCCESS.equals(executeContext.getResponse().getQueryState()) || QueryState.INVALID.equals(executeContext.getResponse().getQueryState())) {
            return Map.of(ExecuteContext.NEXT_KEY, FinishExecutor.NODE_NAME);
        } else if (PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            return Map.of(ExecuteContext.NEXT_KEY, PluginExecutor.NODE_NAME);
        } else if (!Objects.isNull(parseInfo.getSqlInfo())
                && !StringUtils.isBlank(parseInfo.getSqlInfo().getCorrectedS2SQL())) {
            return Map.of(ExecuteContext.NEXT_KEY, SqlExecutor.NODE_NAME);
        } else if ("PLAIN_TEXT".equals(parseInfo.getQueryMode())) {
            return Map.of(ExecuteContext.NEXT_KEY, PlainTextExecutor.NODE_NAME);
        } else {
            return Map.of(ExecuteContext.NEXT_KEY, FinishExecutor.NODE_NAME);
        }
    }
}
