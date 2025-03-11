package com.tencent.supersonic.chat.server.processor.execute;


import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.SseService;
import com.tencent.supersonic.common.util.ContextUtils;

public class CloseSseProcessor implements ExecuteResultProcessor {

    @Override
    public boolean accept(ExecuteContext executeContext) {
        return executeContext.getRequest().isStream();
    }

    @Override
    public void process(ExecuteContext executeContext) {
        SseService sseService = ContextUtils.getBean(SseService.class);
        sseService.closeConn(executeContext.getRequest().getClientId());
    }
}
