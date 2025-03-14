package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.SseService;
import com.tencent.supersonic.common.util.ContextUtils;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FinishExecutor extends ExecutorAgent {

    public static final String NODE_NAME = "FinishExecutor";

    @Override
    public String name() {
        return NODE_NAME;
    }

    @Override
    public Map<String, Object> apply(ExecuteContext executeContext) throws Exception {
        if (executeContext.getRequest().isStream()) {
            String clientId = executeContext.getRequest().getClientId();
            ContextUtils.getBean(SseService.class).closeConn(clientId);
        }
        return Map.of();
    }

}
