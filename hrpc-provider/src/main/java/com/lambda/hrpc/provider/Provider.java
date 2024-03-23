package com.lambda.hrpc.provider;

import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class Provider {
    private Registry registry;
    private String protocolType;
    private Map<String, Object> protocolArgs;
    private static Provider provider;
    private List<RegistryInfo> registryInfoList;
    private Map<String, Map<String, Object>> localServicesCache;
    
    public static Provider providerSingleTon(String registryType, Map<String, Object> registryArgs, String protocolType, Map<String, Object> protocolArgs) {
        if (provider == null) {
            synchronized (Provider.class) {
                if (provider == null) {
                    provider = new Provider(registryType, registryArgs, protocolType, protocolArgs);
                }
            }
        }
        return provider;
    }
    
    private Provider(String registryType, Map<String, Object> registryArgs, String protocolType, Map<String, Object> protocolArgs) {
        this.registryInfoList = new ArrayList<>();
        this.registry = ExtensionLoader.getExtensionLoader(Registry.class).getExtension(registryType, registryArgs);
        this.protocolType = protocolType;
        this.protocolArgs = protocolArgs;
        localServicesCache = new HashMap<>();
    }
    
    public Provider addRegistryInfo(RegistryInfo registryInfo, Object service) {
        this.registryInfoList.add(registryInfo);
        this.localServicesCache
                .computeIfAbsent(registryInfo.getServiceName(), k1 -> new HashMap<>())
                .computeIfAbsent(registryInfo.getVersion(), k2 -> service);
        return this;
    }
    
    public void startProvider() {
        if (this.registry == null) {
            throw new HrpcRuntimeException("the registry hasn't been set yet");
        }

        if (StringUtils.isEmpty(this.protocolType)) {
            throw new HrpcRuntimeException("the protocol hasn't been set yet");
        }
        Set<Integer> portSet = new HashSet<>();
        Integer port = 1010;
        for (RegistryInfo registryInfo : registryInfoList) {
            while (portSet.contains(port)) {
                port = findUnusedPort();
                portSet.add(port);
            }
            registryInfo.setPort(String.valueOf(port));
            if (StringUtils.isEmpty(registryInfo.getHost())) {
                try {
                    registryInfo.setHost(InetAddress.getLocalHost().getHostAddress());
                } catch (UnknownHostException e) {
                    throw new HrpcRuntimeException(e);
                }
            }
            this.registry.registService(registryInfo);
            this.startSingleService(port);
        }
    }
    
    private Integer findUnusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            // 使用构造函数传入0作为端口号，让操作系统自动分配一个可用的端口
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new HrpcRuntimeException(e);
        }
    }

    private void startSingleService(Integer port) {
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(this.protocolType, this.protocolArgs);
        protocol.startNewServer(port);
    }
}
