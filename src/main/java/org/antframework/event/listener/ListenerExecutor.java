/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2016-12-16 01:14 创建
 */
package org.antframework.event.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antframework.event.extension.EventTypeResolver;
import org.antframework.event.extension.ListenResolver;
import org.antframework.event.extension.ListenerType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 监听器执行器
 */
@AllArgsConstructor
public class ListenerExecutor {
    // 监听器类型
    @Getter
    private final Class<? extends ListenerType> type;
    // 优先级
    @Getter
    private final int priority;
    // 监听器
    @Getter
    private final Object listener;
    // 事件类型解决器
    private final EventTypeResolver resolver;
    // 监听执行器map（key：被监听的事件类型）
    private final Map<Object, ListenExecutor> listenExecutorMap;

    /**
     * 执行监听事件
     *
     * @param event 事件
     * @throws Throwable 执行过程中发生任何异常都会往外抛
     */
    public void execute(Object event) throws Throwable {
        ListenExecutor listenExecutor = listenExecutorMap.get(resolver.resolve(event));
        if (listenExecutor != null) {
            listenExecutor.execute(listener, event);
        }
    }

    /**
     * 获取指定优先级顺序的监听事件类型
     *
     * @param priorityType 优先级类型
     */
    public Set<Object> getEventTypes(PriorityType priorityType) {
        Set<Object> eventTypes = new HashSet<>();
        listenExecutorMap.forEach((eventType, listenExecutor) -> {
            if (listenExecutor.getPriorityType() == priorityType) {
                eventTypes.add(eventType);
            }
        });
        return eventTypes;
    }

    /**
     * 监听执行器
     */
    @AllArgsConstructor
    public static class ListenExecutor {
        // 监听解决器
        private final ListenResolver resolver;
        // 是否优先级升序
        @Getter
        private final PriorityType priorityType;
        // 监听方法
        private final Method listenMethod;

        /**
         * 执行监听
         *
         * @param listener 监听器
         * @param event    事件
         * @throws Throwable 执行过程中发生任何异常都会往外抛
         */
        public void execute(Object listener, Object event) throws Throwable {
            try {
                listenMethod.invoke(listener, resolver.resolveParams(event));
            } catch (InvocationTargetException e) {
                // 抛出原始异常
                throw e.getTargetException();
            }
        }

        /**
         * 获取事件类型
         */
        public Object getEventType() {
            return resolver.getEventType();
        }
    }
}
