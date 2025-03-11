package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.server.service.SseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class SseServiceImpl implements SseService {

    private static final Map<String, SseEmitter> SSE_CACHE = new ConcurrentHashMap<>();

    @Value("${spring.mvc.async.request-timeout:600000}")
    private Long timeOut;


    @Override
    public SseEmitter getConn(String clientId) {
        final SseEmitter sseEmitter = SSE_CACHE.get(clientId);

        if (sseEmitter != null) {
            return sseEmitter;
        } else {
            final SseEmitter emitter = new SseEmitter(timeOut);
            // 注册超时回调，超时后触发
            emitter.onTimeout(() -> {
                System.out.println("连接已超时，正准备关闭，clientId = " + clientId);
                SSE_CACHE.remove(clientId);
            });
            // 注册完成回调，调用 emitter.complete() 触发
            emitter.onCompletion(() -> {
                System.out.println("连接已关闭，正准备释放，clientId = " + clientId);
                SSE_CACHE.remove(clientId);
                System.out.println("连接已释放，clientId = " + clientId);
            });
            // 注册异常回调，调用 emitter.completeWithError() 触发
            emitter.onError(throwable -> {
                System.out.println("连接已异常，正准备关闭，clientId = " + clientId + "==>" + throwable);
                SSE_CACHE.remove(clientId);
            });
            SSE_CACHE.put(clientId, emitter);
            return emitter;
        }
    }

    /**
     * 模拟类似于 chatGPT 的流式推送回答
     *
     * @param clientId 客户端 id
     */
    @Override
    public void send(String clientId, SseEmitter.SseEventBuilder eventBuilder) {
        if (SSE_CACHE.containsKey(clientId)) {
            final SseEmitter emitter = SSE_CACHE.get(clientId);
            try {
                emitter.send(eventBuilder);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }

    @Override
    public void closeConn(String clientId) {
        final SseEmitter sseEmitter = SSE_CACHE.get(clientId);
        if (sseEmitter != null) {
            sseEmitter.complete();
        }
    }
}
