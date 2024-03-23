package com.lambda.hrpc.provider;

import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Provider {
    private Registry registry;
    private String protocolType;
    private Object[] protocolArgs;
    
    private List<RegistryInfo> registryInfoList;
    
    public Provider() {
        this.registryInfoList = new ArrayList<>();
    }
    
    public Provider setRegistry(Registry registry) {
        this.registry = registry;
        return this;
    }
    
    public Provider addRegistryInfo(RegistryInfo registryInfo) {
        this.registryInfoList.add(registryInfo);
        return this;
    }
    
    public Provider setProtocol(String protocolType, Object... args) {
        this.protocolType = protocolType;
        this.protocolArgs = args;
        return this;
    }

    public void startProvider(Integer port) {
        if (this.registry == null) {
            throw new HrpcRuntimeException("the registry hasn't been set yet");
        }
        
        if (StringUtils.isEmpty(this.protocolType)) {
            throw new HrpcRuntimeException("the protocol hasn't been set yet");
        }
        
        for (RegistryInfo registryInfo : registryInfoList) {
            this.registry.registService(registryInfo);
        }
        
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(this.protocolType, this.protocolArgs);
        protocol.startNewServer(port);
    }
}
