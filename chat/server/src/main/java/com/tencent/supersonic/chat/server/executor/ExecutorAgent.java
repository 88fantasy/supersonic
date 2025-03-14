package com.tencent.supersonic.chat.server.executor;


import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import org.bsc.langgraph4j.action.NodeAction;

public abstract class ExecutorAgent implements NodeAction<ExecuteContext> {

    public abstract String name();

}
