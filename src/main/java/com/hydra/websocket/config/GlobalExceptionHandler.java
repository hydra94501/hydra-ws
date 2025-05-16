package com.hydra.websocket.config;

import com.hydra.websocket.bean.Result;
import com.hydra.websocket.exceptions.BizException;
import com.hydra.websocket.exceptions.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 全局异常拦截
    @ExceptionHandler
    public Result<String> handlerException(Exception e) {
        if (e instanceof PaymentException) {
            return Result.fail(e.getMessage());
        }
        if (e instanceof BizException) {
            return Result.fail(e.getMessage());
        }
        log.error("系统异常", e);
        return Result.fail("system error");
    }
}
