package com.hydra.websocket.server;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.hydra.websocket.server.WebSocketServer.localClients;


@Slf4j
@Component
public class WebSocketMessageUtil {

    @Autowired
    private RedisWebSocketManager redisManager;

    @Autowired
    private ExecutorService executorService;


    public void sendLocalMessage(WebSocketMessage wsMessage) {
        // 解析消息
        if ("broadcast".equals(wsMessage.getType())) {
            // 广播消息给本实例的所有客户端
            localClients.values().forEach(client ->
                    executorService.submit(() -> {
                        client.sendMessage(JSON.toJSONString(wsMessage.getData()));
                     })
            );
        } else if ("single".equals(wsMessage.getType())) {
            // 检查目标客户端是否在本实例
            String targetDeviceId = wsMessage.getTarget();
            WebSocketServer targetClient = localClients.get(targetDeviceId);
            if (targetClient != null) {
                targetClient.sendMessage(JSON.toJSONString(wsMessage.getData()));
            }
        } else if ("topic".equals(wsMessage.getType())) {
            // 检查目标客户端是否在本实例
            sendSubjectMessage(wsMessage.getTarget(), JSON.toJSONString(wsMessage.getData()));
        }
    }

    public void sendSubjectMessage(String subject, String message) {
        // 从Redis获取订阅了该主题的设备ID列表
        Set<String> deviceIds = redisManager.getDevicesBySubject(subject);
        deviceIds.forEach(deviceId -> {
            WebSocketServer client = localClients.get(deviceId);
            if (client != null) {
                executorService.submit(() -> {
                    client.sendMessage(message);
                });

            }
        });
    }

    public void sendRemoteMessage(WebSocketMessage wsMessage) {
        Set<String> serverList = redisManager.getServerList();
        for (String host : serverList) {
            try {
                executorService.submit(() -> {
                    HttpUtil.post(host + "/shortVideo/api/websocket/sendMsg", JSON.toJSONString(wsMessage));
                });
            } catch (Exception e) {
                log.error("发送远程消息失败：host={}, error={}", host, e.getMessage());
            }
        }
    }

}
