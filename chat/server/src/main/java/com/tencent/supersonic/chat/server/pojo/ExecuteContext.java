package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.executor.FinishExecutor;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Getter;
import lombok.Setter;
import org.bsc.langgraph4j.serializer.std.JsonObjectSerializer;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AppenderChannel;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.MapChannel;
import org.bsc.langgraph4j.state.ReplaceChannel;

import java.util.ArrayList;
import java.util.Map;

@Getter
@Setter
public class ExecuteContext extends AgentState {

    public static final String REQUEST_KEY = "request";

    public static final String RESPONSE_KEY = "response";

    public static final String AGENT_KEY = "agent";

    public static final String PARSE_KEY = "parse";

    public static final String ATTRIBUTES_KEY = "attributes";

    public static final String MESSAGES_KEY = "messages";

    public static final String NEXT_KEY = "next";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            REQUEST_KEY, new ReplaceChannel<>(ChatExecuteReq.class),
            AGENT_KEY, new ReplaceChannel<>(Agent.class),
            PARSE_KEY, new ReplaceChannel<>(SemanticParseInfo.class),
            RESPONSE_KEY, new ReplaceChannel<>(QueryResult.class),
            ATTRIBUTES_KEY, new MapChannel(true),
            MESSAGES_KEY, AppenderChannel.of(ArrayList::new),
            NEXT_KEY, new ReplaceChannel<>(String.class)
    );

    public ExecuteContext(Map<String, Object> initData) {
        super(initData);
    }

    public ChatExecuteReq getRequest() {
        return this.<ChatExecuteReq>value(REQUEST_KEY).orElse(null);
    }

    public QueryResult getResponse() {
        return this.<QueryResult>value(RESPONSE_KEY).orElse(null);
    }

    public Agent getAgent() {
        return this.<Agent>value(AGENT_KEY).orElse(null);
    }

    public SemanticParseInfo getParseInfo() {
        return this.<SemanticParseInfo>value(PARSE_KEY).orElse(null);
    }

    public String getNext() {
        return this.<String>value(NEXT_KEY).orElse(FinishExecutor.NODE_NAME);
    }

    public static class ExecuteContextSerializer extends ObjectStreamStateSerializer<ExecuteContext> {

        public ExecuteContextSerializer() {
            super(ExecuteContext::new);

            mapper().register(ChatExecuteReq.class, new JsonObjectSerializer<>(ChatExecuteReq.class));
            mapper().register(QueryResult.class, new JsonObjectSerializer<>(QueryResult.class));
            mapper().register(Agent.class, new JsonObjectSerializer<>(Agent.class));
            mapper().register(SemanticParseInfo.class, new JsonObjectSerializer<>(SemanticParseInfo.class));

        }
    }
}
