package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.pojo.ExecuteContext;

public interface ChatQueryExecutor {

    void execute(ExecuteContext executeContext);
}
