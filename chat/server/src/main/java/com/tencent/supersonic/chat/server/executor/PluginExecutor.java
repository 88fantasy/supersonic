package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(PluginExecutor.NODE_NAME)
public class PluginExecutor extends NodeComponent {

    public static final String NODE_NAME = "PluginExecutor";

    @Override
    public void process() throws Exception {
        ExecuteContext executeContext = this.getContextBean(ExecuteContext.class);
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if(!executeContext.hasResponse() && PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            PluginSemanticQuery query = PluginQueryManager.getPluginQuery(parseInfo.getQueryMode());
            query.setParseInfo(parseInfo);
            executeContext.setResponse(query.build());
        }

    }
}
