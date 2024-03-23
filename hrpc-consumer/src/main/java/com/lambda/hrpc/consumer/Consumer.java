package com.lambda.hrpc.consumer;


import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        // TODO: loadBalance
        RegistryInfo registryInfo = services.get(0);
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
    }
    
}
