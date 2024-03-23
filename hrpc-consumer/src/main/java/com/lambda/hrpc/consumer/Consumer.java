package com.lambda.hrpc.consumer;


import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;

import java.util.ArrayList;
import java.util.List;

public class Consumer<T> {
    private Registry registry;
    private Protocol protocol;
    public Consumer() {
        
    }
    
    public Consumer setRegistry(Registry registry) {
        this.registry = registry;
        return this;
    }
    
    public Consumer setProtocol(Protocol protocol) {
        this.protocol = protocol;
        return this;
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
