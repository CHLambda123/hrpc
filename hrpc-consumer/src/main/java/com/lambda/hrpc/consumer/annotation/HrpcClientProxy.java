package com.lambda.hrpc.consumer.annotation;

import com.google.protobuf.Message;
import com.lambda.hrpc.consumer.Consumer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class HrpcClientProxy implements InvocationHandler {
    private final Consumer consumer;
    private final Object target;
    public HrpcClientProxy(Consumer consumer, Object target) {
        this.consumer = consumer;
        this.target = target;
    }
    
    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        HrpcClient annotation = this.target.getClass().getAnnotation(HrpcClient.class);
        String serviceName = annotation.serviceName();
        String version = annotation.version();
        Message[] messages = Arrays.copyOf(args, args.length, Message[].class);
        return consumer.requestService(serviceName, version, method.getName(), method.getReturnType(), messages);
    }
    public <T> T createProxy(Class<T> tClass) {
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass}, this);
    }
}
