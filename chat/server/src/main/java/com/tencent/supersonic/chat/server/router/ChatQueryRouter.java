package com.tencent.supersonic.chat.server.router;


import com.tencent.supersonic.chat.server.executor.PlainTextExecutor;
import com.tencent.supersonic.chat.server.executor.PluginExecutor;
import com.tencent.supersonic.chat.server.executor.SqlExecutor;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.EdgeAction;

import java.util.Objects;

public class ChatQueryRouter implements EdgeAction<ExecuteContext> {


    @Override
    public String apply(ExecuteContext executeContext) throws Exception {
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if (PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            return PluginExecutor.NODE_NAME;
        } else if (!Objects.isNull(parseInfo.getSqlInfo())
                && !StringUtils.isBlank(parseInfo.getSqlInfo().getCorrectedS2SQL())) {
            return SqlExecutor.NODE_NAME;
        } else {
            return PlainTextExecutor.NODE_NAME;
        }
    }
}
