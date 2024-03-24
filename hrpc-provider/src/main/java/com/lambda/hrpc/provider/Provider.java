package com.lambda.hrpc.provider;

import com.lambda.hrpc.common.annotation.AnnotationUtil;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import com.lambda.hrpc.provider.annotation.HrpcService;
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
    
    public static Provider providerSingleTon(Registry registry, String protocolType, Map<String, Object> protocolArgs, String packageName) {
        if (provider == null) {
            synchronized (Provider.class) {
                if (provider == null) {
                    provider = new Provider(registry, protocolType, protocolArgs, packageName);
                }
            }
        }
        return provider;
    }
    
    private Provider(Registry registry, String protocolType, Map<String, Object> protocolArgs, String packageName) {
        this.registry = registry;
        this.protocolType = protocolType;
        this.protocolArgs = protocolArgs;
        registerLocalServices(packageName);
    }
    
    private void registerLocalServices(String packageName) {
        this.localServicesCache = new HashMap<>();
        this.protocolArgs.put("localServicesCache", this.localServicesCache);
        this.registryInfoList = new ArrayList<>();
        List<Class<?>> classes = AnnotationUtil.scanAnnotation(packageName, HrpcService.class);
        for (Class<?> aClass : classes) {
            HrpcService hrpcServiceAnn = aClass.getAnnotation(HrpcService.class);
            String serviceName = hrpcServiceAnn.serviceName();
            String version = hrpcServiceAnn.version();
            int weight = hrpcServiceAnn.weight();
            String host = hrpcServiceAnn.host();
            RegistryInfo registryInfo = new RegistryInfo(serviceName, version, host, weight);
            try {
                Object service = aClass.getConstructor().newInstance();
                this.registryInfoList.add(registryInfo);
                this.localServicesCache
                        .computeIfAbsent(registryInfo.getServiceName(), k1 -> new HashMap<>())
                        .computeIfAbsent(registryInfo.getVersion(), k2 -> service);
            } catch (Exception e) {
                throw new HrpcRuntimeException(e);
            }
        }
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
            }
            portSet.add(port);
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
