package com.hydra.websocket.server.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Msg {
    private String type;        // 消息类型：ping : 心跳消息, biz: 业务消息 ,pull: 拉取消息
    private String subject;     // 消息主题：gameRecommend 游戏推荐
    private Object data;        // 消息内容
    private Long timestamp;   // 消息时间戳

    public Msg(String subject, Object data) {
        this.type="biz";
        this.subject = subject;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}
