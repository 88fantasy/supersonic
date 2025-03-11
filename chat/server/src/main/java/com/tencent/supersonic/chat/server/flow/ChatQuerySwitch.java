package com.tencent.supersonic.chat.server.flow;


import com.tencent.supersonic.chat.server.executor.PlainTextExecutor;
import com.tencent.supersonic.chat.server.executor.PluginExecutor;
import com.tencent.supersonic.chat.server.executor.SqlExecutor;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@LiteflowComponent(ChatQuerySwitch.NONE_NAME)
public class ChatQuerySwitch extends NodeSwitchComponent {

    public final static String NONE_NAME = "ChatQuerySwitch";

    @Override
    public String processSwitch() throws Exception {
        ExecuteContext executeContext = this.getContextBean(ExecuteContext.class);
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if (PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            return "chatQueryPluginExecuteChain";
        } else if (!Objects.isNull(parseInfo.getSqlInfo())
                && !StringUtils.isBlank(parseInfo.getSqlInfo().getCorrectedS2SQL())) {
            return "chatQuerySqlExecuteChain";
        } else {
            return "chatQueryPlainExecuteChain";
        }
    }
}
