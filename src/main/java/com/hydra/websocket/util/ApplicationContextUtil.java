package com.hydra.websocket.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextUtil implements ApplicationContextAware {
    private static ApplicationContext context;

    public ApplicationContextUtil() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return null == context ? null : context.getBean(clazz);
    }

    public static <T> T getBean(String beanName) {
        return null == context ? null : (T)context.getBean(beanName);
    }

    public static <T> T getBean(String beanName, Class<T> clazz) {
        return null == context ? null : context.getBean(beanName, clazz);
    }
}
