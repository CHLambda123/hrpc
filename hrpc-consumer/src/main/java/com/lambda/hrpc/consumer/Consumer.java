package com.lambda.hrpc.consumer;


import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import com.lambda.hrpc.consumer.loadbalance.LoadBalance;
import com.lambda.hrpc.consumer.loadbalance.SimpleLoadBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Consumer {
    private Registry registry;
    private Protocol protocol;
    private static Consumer consumer;
    
    private Consumer(String registryType, Map<String, Object> registryArgs, String protocolType, Map<String, Object> protocolArgs) {
        this.registry = ExtensionLoader.getExtensionLoader(Registry.class).getExtension(registryType, registryArgs);
        this.protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolType, protocolArgs);
    }
    
    public static Consumer consumerSingleTon(String registryType, Map<String, Object> registryArgs, String protocolType, Map<String, Object> protocolArgs) {
        if (consumer == null) {
            synchronized (Consumer.class) {
                if (consumer == null) {
                    consumer = new Consumer(registryType, registryArgs, protocolType, protocolArgs);
                }
            }
        }
        return consumer;
    }
    
    public <T> T getConsumer(String serviceName, String version, String methodName, Class<T> returnType, Message... params) {
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
