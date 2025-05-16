package com.hydra.websocket.server;

import com.alibaba.fastjson.JSON;
import com.hydra.websocket.server.message.Msg;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/ws/{deviceId}")
@Component
@Slf4j
@Data
public class WebSocketServer  {

    private static RedisWebSocketManager redisManager;
    private static String serverId;
    public static final Map<String, WebSocketServer> localClients = new ConcurrentHashMap<>();
    
    // 添加心跳超时相关常量和变量
    private static final long HEARTBEAT_TIMEOUT = 60000; // 心跳超时时间，单位毫秒
    private long lastHeartbeatTime; // 最后一次心跳时间
    private static ScheduledExecutorService heartbeatScheduler; // 心跳检测调度器
    
    private Session session;
    private String deviceId;
    
    // 静态初始化块，创建心跳检测调度器
    static {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("websocket-heartbeat-thread");
            thread.setDaemon(true);
            return thread;
        });
        
        // 启动定时任务，每30秒检查一次所有连接的心跳状态
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            checkAllHeartbeats();
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    @Autowired
    public void setRedisManager(RedisWebSocketManager manager) {
        redisManager = manager;
    }

    @Value("http://127.0.0.1:${server.port:8080}")
    public void setServerId(String id) {
        serverId = id;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("deviceId") String deviceId) throws IOException {
        this.session = session;
        this.deviceId = deviceId;
        this.lastHeartbeatTime = System.currentTimeMillis(); // 初始化心跳时间
        
        // 注册到本地和Redis
        localClients.put(deviceId, this);
        redisManager.registerClient(deviceId, serverId);
        
        log.info("WebSocket收到新连接！设备ID：{}，服务器ID：{}，当前在线数：{}", 
                this.deviceId, serverId, redisManager.getOnlineCount());

        Msg msg = new Msg("ping", "connection","链接成功", System.currentTimeMillis());
        session.getBasicRemote().sendText(JSON.toJSONString(msg));
        
        // 设置会话超时时间
        session.setMaxIdleTimeout(HEARTBEAT_TIMEOUT * 2);
    }

    @OnClose
    public void onClose() {
        if (StringUtils.isEmpty(this.deviceId)) {
            return;
        }
        // 从本地和Redis移除
        localClients.remove(this.deviceId);
        redisManager.unregisterClient(this.deviceId, serverId);
        
        log.info("WebSocket连接关闭！设备ID：{}，服务器ID：{}，当前在线数：{}", 
                this.deviceId, serverId, redisManager.getOnlineCount());
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        if("ping".equals(message)){
            session.getBasicRemote().sendText("pong");
            updateHeartbeat(); // 更新心跳时间
            return;
        }
        updateHeartbeat(); // 更新心跳时间
        log.info("WebSocket收到消息！设备ID：{}，消息内容：{}", this.deviceId, message);
        Msg receiveMsg =  JSON.parseObject(message, Msg.class);
        if(receiveMsg.getType().equals("ping")){
            Msg msg = new Msg("pong", "connection","成功收到消息", System.currentTimeMillis());
            session.getBasicRemote().sendText(JSON.toJSONString(msg));
        } else if ("subscribe".equals(receiveMsg.getType())) {
            // 订阅主题
            String subject = receiveMsg.getSubject();
            subscribeToSubject(subject);
        }else if ("unSubscribe".equals(receiveMsg.getType())) {
            // 取消订阅
            String subject = receiveMsg.getSubject();
            unSubscribeToSubject(subject);
        }
    }
    
    // 更新最后心跳时间
    private void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
        log.debug("更新设备心跳时间：{}", this.deviceId);
    }
    
    // 检查心跳是否超时
    private boolean isHeartbeatTimeout() {
        return System.currentTimeMillis() - this.lastHeartbeatTime > HEARTBEAT_TIMEOUT;
    }
    
    // 检查所有连接的心跳状态
    private static void checkAllHeartbeats() {
        log.debug("开始检查所有WebSocket连接的心跳状态，当前连接数：{}", localClients.size());
        // 创建需要关闭的连接列表，避免在遍历过程中修改集合
        List<WebSocketServer> timeoutClients = new ArrayList<>();
        // 检查所有连接
        for (WebSocketServer client : localClients.values()) {
            if (client.isHeartbeatTimeout()) {
                timeoutClients.add(client);
            }
        }
        // 关闭超时的连接
        for (WebSocketServer client : timeoutClients) {
            try {
                log.info("检测到心跳超时，关闭连接：{}", client.deviceId);
                client.session.close(new CloseReason(
                        CloseReason.CloseCodes.NORMAL_CLOSURE, 
                        "心跳超时，连接关闭"));
            } catch (IOException e) {
                log.error("关闭超时连接失败：{}, 错误：{}", client.deviceId, e.getMessage());
            }
        }
        if (!timeoutClients.isEmpty()) {
            log.info("心跳检测完成，关闭了{}个超时连接", timeoutClients.size());
        }
    }
    


    private void subscribeToSubject(String subject) {
        // 使用Redis将设备ID和主题关联
        redisManager.addDeviceToSubject(this.deviceId, subject);
        log.info("设备ID：{} 订阅了主题：{}", this.deviceId, subject);
    }

    private void unSubscribeToSubject(String subject) {
        // 使用Redis将设备ID和主题关联
        redisManager.removeDeviceToSubject(this.deviceId, subject);
        log.info("设备ID：{} 取消订阅了主题：{}", this.deviceId, subject);
    }



    @OnError
    public void onError(Throwable throwable) {
        log.error("WebSocket连接发生错误！设备ID：{}，异常信息：{}", this.deviceId, throwable);
        try {
            this.session.close(new CloseReason(
                    CloseReason.CloseCodes.CLOSED_ABNORMALLY, 
                    "WebSocket连接发生错误"));
        } catch (IOException e) {
            log.error("WebSocket关闭发生错误！设备ID：{}，异常信息：{}", this.deviceId, e);
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
            log.info("消息发送成功 {} 消息内容：{}",this.deviceId,message);
        } catch (IOException e) {
            log.error("消息发送失败！设备ID：{}，消息内容：{}，错误：{}", 
                    this.deviceId, message, e.getMessage());
        }
    }


}
