package com.lambda.hrpc.registry.common;

import com.lambda.hrpc.common.entity.RegistryInfo;
import com.lambda.hrpc.common.spi.Spi;

import java.util.List;
@Spi
public interface Registry {
    void registService(RegistryInfo registryInfo);
    void unregister(RegistryInfo registryInfo);

    List<RegistryInfo> getServices(String serviceName, String version);

}
