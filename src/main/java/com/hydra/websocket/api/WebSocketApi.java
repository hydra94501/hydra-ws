package com.hydra.websocket.api;

import com.hydra.websocket.server.WebSocketMessage;
import com.hydra.websocket.server.WebSocketMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RequestMapping("/websocket/api")
@RestController
public class WebSocketApi {

    @Autowired
    private WebSocketMessageUtil webSocketMessageUtil;

    @PostMapping("/sendMsg")
    public String sendMsg(@RequestBody WebSocketMessage webSocketMessage) throws IOException {
        webSocketMessageUtil.sendLocalMessage(webSocketMessage);
        return "SUCCESS";
    }

    @GetMapping("/hello")
    public String getHello() {
        return "hello";
    }

}
