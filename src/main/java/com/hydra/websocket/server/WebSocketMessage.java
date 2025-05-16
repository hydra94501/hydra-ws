package com.hydra.websocket.server;

import com.hydra.websocket.server.message.Msg;
import lombok.Data;

@Data
public class WebSocketMessage<T extends Msg> {
    private String type;        // 消息类型：broadcast、single,topic
    private T data;     // 消息内容
    private String target;      // 目标设备ID（单发消息时使用）, topic 发送主题时候使用
    private String source;      // 源设备ID

    public WebSocketMessage() {
    }

    public WebSocketMessage(String type, T data) {
        this.type = type;
        this.data = data;
    }

    public WebSocketMessage(String type, T data, String target) {
        this.type = type;
        this.data = data;
        this.target = target;
    }
}