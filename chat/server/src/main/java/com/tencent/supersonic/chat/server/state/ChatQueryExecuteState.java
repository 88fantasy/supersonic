//package com.tencent.supersonic.chat.server.state;
//
//
//import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
//import org.bsc.langgraph4j.state.AgentState;
//import org.bsc.langgraph4j.state.AppenderChannel;
//import org.bsc.langgraph4j.state.Channel;
//import org.bsc.langgraph4j.state.Reducer;
//
//import java.util.ArrayList;
//import java.util.Map;
//import java.util.Optional;
//import java.util.function.Supplier;
//
//public class ChatQueryExecuteState extends AgentState {
//
//    public static final String CONTEXT_KEY = "context";
//
//    public static final String REASONING_KEY = "reasoning";
//
//    public static final String MESSAGE_KEY = "messages";
//
//    public static final Map<String, Channel<?>> SCHEMA = Map.of(
//            CONTEXT_KEY, new Channel<ExecuteContext>() {
//                @Override
//                public Optional<Reducer<ExecuteContext>> getReducer() {
//                    return Optional.of((oldValue, newValue) -> newValue);
//                }
//
//                @Override
//                public Optional<Supplier<ExecuteContext>> getDefault() {
//                    return Optional.of(ExecuteContext::new);
//                }
//            },
//            REASONING_KEY, AppenderChannel.of(ArrayList::new),
//            MESSAGE_KEY, AppenderChannel.of(ArrayList::new)
//    );
//
//
//    /**
//     * Constructs an AgentState with the given initial data.
//     *
//     * @param initData the initial data for the agent state
//     */
//    public ChatQueryExecuteState(Map<String, Object> initData) {
//        super(initData);
//    }
//
//    public ExecuteContext context() {
//        return this.<ExecuteContext>value(CONTEXT_KEY).orElse(null);
//    }
//
//}
