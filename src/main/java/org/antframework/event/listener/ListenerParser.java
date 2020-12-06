/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2016-12-16 01:14 创建
 */
package org.antframework.event.listener;

import lombok.extern.slf4j.Slf4j;
import org.antframework.event.annotation.listener.Listen;
import org.antframework.event.annotation.listener.Listener;
import org.antframework.event.extension.EventTypeResolver;
import org.antframework.event.extension.ListenResolver;
import org.antframework.event.extension.ListenerType;
import org.antframework.event.listener.ListenerExecutor.ListenExecutor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * 监听器解析器
 */
@Slf4j
public final class ListenerParser {
    /**
     * 解析监听器
     *
     * @param listener 监听器
     * @return 监听器执行器
     */
    public static ListenerExecutor parseListener(Object listener) {
        // 获取目标class（应对AOP代理情况）
        Class<?> listenerClass = AopUtils.getTargetClass(listener);
        log.debug("解析监听器：{}", listenerClass);
        Listener listenerAnnotation = AnnotatedElementUtils.findMergedAnnotation(listenerClass, Listener.class);
        // 解析
        EventTypeResolver resolver = parseToEventTypeResolver(listenerAnnotation.type());
        Map<Object, ListenExecutor> listenExecutorMap = parseToListenExecutors(listenerClass);

        return new ListenerExecutor(listenerAnnotation.type(), listenerAnnotation.priority(), listener, resolver, listenExecutorMap);
    }

    /**
     * 解析出事件类型解决器
     *
     * @param type 监听器类型
     */
    public static EventTypeResolver parseToEventTypeResolver(Class<? extends ListenerType> type) {
        ListenerType listenerType = BeanUtils.instantiate(type);
        return listenerType.getResolver();
    }

    // 解析出所有监听方法
    private static Map<Object, ListenExecutor> parseToListenExecutors(Class<?> listenerClass) {
        Map<Object, ListenExecutor> map = new HashMap<>();
        // 解析
        ReflectionUtils.doWithLocalMethods(listenerClass, method -> {
            Listen listenAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Listen.class);
            if (listenAnnotation != null) {
                ListenExecutor listenExecutor = parseListen(listenAnnotation, method);
                Assert.isTrue(!map.containsKey(listenExecutor.getEventType()), String.format("监听器[%s]存在监听同一个事件类型[%s]的多个方法", listenerClass, listenExecutor.getEventType()));
                map.put(listenExecutor.getEventType(), listenExecutor);
            }
        });

        return map;
    }

    // 解析监听方法
    private static ListenExecutor parseListen(Listen listenAnnotation, Method listenMethod) {
        log.debug("解析监听方法：{}", listenMethod);
        // 校验方法类型
        Assert.isTrue(Modifier.isPublic(listenMethod.getModifiers()), String.format("监听方法[%s]必须是public类型", listenMethod));
        // 创建监听解决器
        ListenResolver resolver = BeanUtils.instantiate(listenAnnotation.resolver());
        resolver.init(listenMethod);

        return new ListenExecutor(resolver, listenAnnotation.priorityType(), listenMethod);
    }
}
