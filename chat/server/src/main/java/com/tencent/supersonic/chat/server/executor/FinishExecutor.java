package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.SseService;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.beans.factory.annotation.Autowired;

@LiteflowComponent(FinishExecutor.NODE_NAME)
public class FinishExecutor extends NodeComponent {

    public static final String NODE_NAME = "FinishExecutor";

    @Autowired
    SseService sseService;

    @Override
    public void process() throws Exception {
        ExecuteContext executeContext = this.getContextBean(ExecuteContext.class);

        if(executeContext.getRequest().isStream()) {
            String clientId = executeContext.getRequest().getClientId();
            sseService.closeConn(clientId);
        }
    }
}
