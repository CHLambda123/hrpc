package com.lambda.hrpc.common.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@AllArgsConstructor
@ToString
public class RegistryInfo {
    private String serviceName;
    private String version;
    private String host;
    private String port;
    private int weight = 1;
    public RegistryInfo(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;
    }
    public RegistryInfo(String serviceName, String version, String host) {
        this.serviceName = serviceName;
        this.version = version;
        this.host = host;
    }
    public RegistryInfo(String serviceName, String version, String host, String port) {
        this.serviceName = serviceName;
        this.version = version;
        this.host = host;
        this.port = port;
    }
}
