package org.bsc.langgraph4j.langchain4j.generators;


import dev.langchain4j.model.StreamingReasoningResponseHandler;
import dev.langchain4j.model.output.Response;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.streaming.StreamingOutput;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

@Slf4j
public class StreamingReasoningChatGenerator <State extends AgentState, T> extends AsyncGenerator.WithResult<StreamingOutput<State>> {

    /**
     * Creates a new Builder instance for LLMStreamingGenerator.
     *
     * @param <State> the type of the state extending AgentState
     * @return a new Builder instance
     */
    public static <State extends AgentState, T> StreamingReasoningChatGenerator.Builder<State,T> builder() {
        return new StreamingReasoningChatGenerator.Builder<>();
    }

    final StreamingReasoningResponseHandler<T> handler;

    /**
     * Constructs an LLMStreamingGenerator with the specified parameters.
     *
     * @param queue the blocking queue for async generator data
     * @param startingNode the starting node for streaming
     * @param startingState the initial state
     * @param mapResult a function to map the response to a Map (ie. Partial State )
     */
    private StreamingReasoningChatGenerator( @NonNull BlockingQueue<Data<StreamingOutput<State>>> queue,
                                    String startingNode,
                                    State startingState,
                                    Function<Response<T>, Map<String,Object>> mapResult)
    {
        super(new AsyncGeneratorQueue.Generator<>( queue ));

        this.handler = new StreamingReasoningResponseHandler<T>() {

            @Override
            public void onNextReasoning(String token) {
                log.trace("onNextReasoning: {}", token);
                queue.add( AsyncGenerator.Data.of( new StreamingOutput<>( token, startingNode, startingState ) ) );
            }

            @Override
            public void onNext(String token) {
                log.trace("onNext: {}", token);
                queue.add( AsyncGenerator.Data.of( new StreamingOutput<>( token, startingNode, startingState ) ) );

            }

            @Override
            public void onComplete(Response<T> response) {
                log.trace("onComplete: {}", response);
                queue.add(AsyncGenerator.Data.done( mapResult.apply(response) ));
            }

            @Override
            public void onError(Throwable error) {
                log.trace("onError", error);
                queue.add( AsyncGenerator.Data.error(error) );
            }
        };
    }

    /**
     * Returns the StreamingResponseHandler associated with this generator.
     *
     * @return the handler for streaming responses
     */
    public StreamingReasoningResponseHandler<T> handler() {
        return handler;
    }

    /**
     * Builder class for constructing instances of LLMStreamingGenerator.
     *
     * @param <State> the type of the state extending AgentState
     */
    public static class Builder<State extends AgentState, T> {
        private BlockingQueue<AsyncGenerator.Data<StreamingOutput<State>>> queue;
        private Function<Response<T>,  Map<String,Object>> mapResult;
        private String startingNode;
        private State startingState;

        /**
         * Sets the queue for the builder.
         *
         * @param queue the blocking queue for async generator data
         * @return the builder instance
         */
        public StreamingReasoningChatGenerator.Builder<State,T> queue(BlockingQueue<AsyncGenerator.Data<StreamingOutput<State>>> queue ) {
            this.queue = queue;
            return this;
        }

        /**
         * Sets the mapping function for the builder.
         *
         * @param mapResult a function to map the response to a result
         * @return the builder instance
         */
        public StreamingReasoningChatGenerator.Builder<State,T> mapResult(Function<Response<T>, Map<String,Object>> mapResult ) {
            this.mapResult = mapResult;
            return this;
        }

        /**
         * Sets the starting node for the builder.
         *
         * @param node the starting node
         * @return the builder instance
         */
        public StreamingReasoningChatGenerator.Builder<State,T> startingNode(String node ) {
            this.startingNode = node;
            return this;
        }

        /**
         * Sets the starting state for the builder.
         *
         * @param state the initial state
         * @return the builder instance
         */
        public StreamingReasoningChatGenerator.Builder<State,T> startingState(State state ) {
            this.startingState = state;
            return this;
        }

        /**
         * Builds and returns an instance of LLMStreamingGenerator.
         *
         * @return a new instance of LLMStreamingGenerator
         */
        public StreamingReasoningChatGenerator<State,T> build() {
            if( queue == null )
                queue = new LinkedBlockingQueue<>();
            return new StreamingReasoningChatGenerator<>( queue, startingNode, startingState, mapResult );
        }
    }
}