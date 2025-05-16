package com.hydra.websocket.server;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisWebSocketManager {

    private static final String WS_CLIENTS_KEY = "websocket:clients";
    private static final String WS_SERVER_KEY = "websocket:servers";
    private static final String WS_MESSAGE_CHANNEL = "websocket:messages";
    private static final String WS_SUBJECTS_KEY = "websocket:subjects";
    private static final String WS_SUBJECTS_KEYS = "websocket:subjects:list";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 注册WebSocket客户端
     */
    public void registerClient(String deviceId, String serverId) {
        try {
            // 记录设备ID和服务器ID的映射关系
            redisTemplate.opsForHash().put(WS_CLIENTS_KEY, deviceId, serverId);
            // 将设备ID添加到当前服务器的客户端集合中
            redisTemplate.opsForSet().add(WS_SERVER_KEY + ":" + serverId, deviceId);
            log.info("注册WebSocket客户端：deviceId={}, serverId={}", deviceId, serverId);
        } catch (Exception e) {
            log.error("注册WebSocket客户端失败：deviceId={}, serverId={}, error={}", deviceId, serverId, e.getMessage());
        }
    }

    /**
     * 注销WebSocket客户端
     */
    public void unregisterClient(String deviceId, String serverId) {
        try {
            redisTemplate.opsForHash().delete(WS_CLIENTS_KEY, deviceId);
            redisTemplate.opsForSet().remove(WS_SERVER_KEY + ":" + serverId, deviceId);
            log.info("注销WebSocket客户端：deviceId={}, serverId={}", deviceId, serverId);
        } catch (Exception e) {
            log.error("注销WebSocket客户端失败：deviceId={}, serverId={}, error={}", deviceId, serverId, e.getMessage());
        }
    }

    /**
     * 获取客户端所在的服务器ID
     */
    public String getClientServer(String deviceId) {
        return (String) redisTemplate.opsForHash().get(WS_CLIENTS_KEY, deviceId);
    }

    /**
     * 获取服务器的所有客户端
     */
    public Set<String> getServerClients(String serverId) {
        return redisTemplate.opsForSet().members(WS_SERVER_KEY + ":" + serverId);
    }

    /**
     * 获取所有WebSocket服务器ID列表
     */
    public Set<String> getServerList() {
        String pattern = WS_SERVER_KEY + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        return keys.stream()
                .map(key -> key.substring(WS_SERVER_KEY.length() + 1))
                .collect(Collectors.toSet());
    }

    /**
     * 获取在线客户端数量
     */
    public long getOnlineCount() {
        return redisTemplate.opsForHash().size(WS_CLIENTS_KEY);
    }

    /**
     * 发布消息到Redis通道
     */
    public void publishMessage(WebSocketMessage message) {
        try {
            redisTemplate.convertAndSend(WS_MESSAGE_CHANNEL, JSON.toJSONString(message));
        } catch (Exception e) {
            log.error("发布WebSocket消息失败：message={}, error={}", message, e.getMessage());
        }
    }

    /**
     * 添加设备到指定主题
     * @param subject 主题标识（如直播间ID、群组ID等）
     * @param deviceId 设备唯一标识
     */
    public void addDeviceToSubject( String deviceId,String subject) {
        try {
            String key = WS_SUBJECTS_KEY + ":" + subject;
            redisTemplate.opsForSet().add(key, deviceId);
            redisTemplate.opsForSet().add(WS_SUBJECTS_KEYS, subject);
            log.debug("设备加入主题：subject={}, deviceId={}", subject, deviceId);
        } catch (Exception e) {
            log.error("添加设备到主题失败：subject={}, deviceId={}, error={}",
                    subject, deviceId, e.getMessage());
        }
    }


    public Set<String> getAllSubjects() {
        try {
            return redisTemplate.opsForSet().members(WS_SUBJECTS_KEYS);
        } catch (Exception e) {
            return Set.of();
        }
    }


    /**
     * 获取指定主题下的所有设备
     * @param subject 主题标识
     * @return 设备ID集合（可能为空集合）
     */
    public Set<String> getDevicesBySubject(String subject) {
        try {
            String key = WS_SUBJECTS_KEY + ":" + subject;
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("获取主题设备失败：subject={}, error={}", subject, e.getMessage());
            return Set.of();
        }
    }

    public void removeDeviceToSubject(String deviceId, String subject) {
        try {
            String key = WS_SUBJECTS_KEY + ":" + subject;
            redisTemplate.opsForSet().remove(key, deviceId);
            log.debug("设备移除主题：subject={}, deviceId={}", subject, deviceId);
        } catch (Exception e) {
            log.error("设备移除主题失败：subject={}, deviceId={}, error={}",
                    subject, deviceId, e.getMessage());
        }
    }
}