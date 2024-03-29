package com.lambda.hrpc.consumer;


import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.lambda.hrpc.common.annotation.AnnotationUtil;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import com.lambda.hrpc.consumer.annotation.HrpcClient;
import com.lambda.hrpc.consumer.annotation.HrpcClientProxy;
import com.lambda.hrpc.consumer.loadbalance.LoadBalance;
import com.lambda.hrpc.consumer.loadbalance.SimpleLoadBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Consumer {
    private Registry registry;
    private Protocol protocol;
    private static Consumer consumer;
    private List<Object> clients;    
    private Consumer(Registry registry, String protocolType, String packageName) {
        this.registry = registry;
        this.protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolType, null);
        clients = setAllClient(packageName);
    }
    
    public static Consumer consumerSingleTon(Registry registry, String protocolType, String packageName) {
        if (consumer == null) {
            synchronized (Consumer.class) {
                if (consumer == null) {
                    consumer = new Consumer(registry, protocolType, packageName);
                }
            }
        }
        return consumer;
    }
    
    public List<Object> getAllClient() {
        return this.clients;
    }
    
    private List<Object> setAllClient(String packageName) {
        List<Class<?>> classes = AnnotationUtil.scanAnnotation(packageName, HrpcClient.class);
        if (classes.isEmpty()) {
            return new ArrayList<>();
        }
        return classes.stream().map(c -> {
            try {
                // 代理，使得object中的方法最终走requestService方法
                Object target = c.getConstructor().newInstance();
                return new HrpcClientProxy(this, target).createProxy(target.getClass());
            } catch (Exception e) {
                throw new HrpcRuntimeException(e);
            }
        }).collect(Collectors.toList());
    }
    
    public <T> T requestService(String serviceName, String version, String methodName, Class<T> returnType, Message... params) {
        if (registry == null) {
            throw new HrpcRuntimeException("the registry hasn't been set yet");
        }
        if (protocol == null) {
            throw new HrpcRuntimeException("the protocol hasn't been set yet");
        }
        List<RegistryInfo> services = registry.getServices(serviceName, version);
        if (services == null || services.isEmpty()) {
            throw new HrpcRuntimeException("find no services");
        }
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                while (true) {
                    try {
                        LoadBalance loadBalance = new SimpleLoadBalance();
                        RegistryInfo registryInfo = loadBalance.selectOneService(services);
                        List<Any> paramsList = new ArrayList<>();
                        List<String> paramsTypeList = new ArrayList<>();
                        for (Message param : params) {
                            paramsList.add(Any.pack(param));
                            paramsTypeList.add(param.getClass().getName());
                        }
                        String ip = registryInfo.getHost();
                        String port = registryInfo.getPort();
                        Invocation.AppInvocation invocation = Invocation.AppInvocation.newBuilder()
                                .setServiceName(serviceName)
                                .setVersion(version)
                                .setMethodName(methodName)
                                .addAllParamTypes(paramsTypeList)
                                .addAllParams(paramsList)
                                .build();
                        return protocol.executeRequest(ip, Integer.valueOf(port), invocation, returnType);
                    } catch (Exception igE) {

                    }
                }
            }
        });

        try {
            // 5秒超时
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        } finally {
            // 关闭线程池
            executor.shutdownNow();
        }
    }
    
}
