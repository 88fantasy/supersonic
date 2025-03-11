package com.tencent.supersonic.chat.server.service;


import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {

    SseEmitter getConn(String clientId);

    void send(String clientId, SseEmitter.SseEventBuilder eventBuilder);

    void closeConn(String clientId);
}
