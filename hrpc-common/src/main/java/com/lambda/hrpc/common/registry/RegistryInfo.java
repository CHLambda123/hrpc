package com.lambda.hrpc.common.registry;

import jakarta.validation.constraints.Size;
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
    @Size(min = 1, max = 10, message = "weight must be set between 1 and 10")
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

    public RegistryInfo(String serviceName, String version, int weight) {
        this.serviceName = serviceName;
        this.version = version;
        this.weight = weight;
    }

    public RegistryInfo(String serviceName, String version, String host, int weight) {
        this.serviceName = serviceName;
        this.version = version;
        this.host = host;
        this.weight = weight;
    }
    
    public RegistryInfo(String serviceName, String version, String host, String port) {
        this.serviceName = serviceName;
        this.version = version;
        this.host = host;
        this.port = port;
    }
}
