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
import com.lambda.hrpc.consumer.loadbalance.LoadBalance;
import com.lambda.hrpc.consumer.loadbalance.SimpleLoadBalance;
import com.lambda.hrpc.consumer.proxy.HrpcClient;
import com.lambda.hrpc.consumer.proxy.HrpcClientProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Consumer {
    private final Registry registry;
    private final Protocol protocol;
    private static volatile Consumer consumer;
    private final Map<String, Object> clientsMap;
    private Consumer(Registry registry, String protocolType, String packageName) {
        this.registry = registry;
        this.protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolType, null);
        clientsMap = new HashMap<>();
        setAllClient(packageName);
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
    
    public Map<String, Object> getAllClient() {
        return this.clientsMap;
    }
    
    public <T> T getClient(Class<T> clazz) {
        Object obj = this.clientsMap.get(clazz.getName());
        if (obj == null) {
            throw new HrpcRuntimeException("there has no service for class: " + clazz.getName());
        }
        return (T)obj;
    }
    
    private void setAllClient(String packageName) {
        List<Class<?>> classes = AnnotationUtil.scanAnnotation(packageName, HrpcClient.class);
        for (Class<?> aClass : classes) {
            if (!aClass.isInterface()) {
                continue;
            }
            this.clientsMap.put(aClass.getName(), new HrpcClientProxy(this, aClass).createProxy());
        }
    }
    
    public  <T> T requestService(String serviceName, String version, String methodName, Class<T> returnType, Message... params) {
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
        for (int i = 0; i < services.size(); i++) {
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
            try {
                return protocol.executeRequest(ip, Integer.valueOf(port), invocation, returnType);
            } catch (Exception igE) {
                registryInfo.setWeight(registryInfo.getWeight()*(-1));
            }
        }
        throw new HrpcRuntimeException("there has no service available");

    }    
}
