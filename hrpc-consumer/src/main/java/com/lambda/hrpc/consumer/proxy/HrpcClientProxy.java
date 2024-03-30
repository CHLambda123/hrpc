package com.lambda.hrpc.consumer.proxy;

import com.google.protobuf.Message;
import com.lambda.hrpc.consumer.Consumer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class HrpcClientProxy implements InvocationHandler {
    private final Consumer consumer;
    private final Class<?> targetClazz;
    public HrpcClientProxy(Consumer consumer, Class<?> targetClazz) {
        this.consumer = consumer;
        this.targetClazz = targetClazz;
    }
    
    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        HrpcClient annotation = this.targetClazz.getAnnotation(HrpcClient.class);
        String serviceName = annotation.serviceName();
        String version = annotation.version();
        if (args != null) {
            Message[] messages = Arrays.copyOf(args, args.length, Message[].class);
            return consumer.requestService(serviceName, version, method.getName(), method.getReturnType(), messages);    
        } else {
            return consumer.requestService(serviceName, version, method.getName(), method.getReturnType());
        }
        
    }
    public <T> T createProxy() {
        return (T) Proxy.newProxyInstance(targetClazz.getClassLoader(), new Class[]{targetClazz}, this);
    }
}
