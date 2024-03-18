package com.lambda.registry.common;

import com.lambda.common.entity.RegistryInfo;
import com.lambda.common.spi.Spi;

import java.util.List;
@Spi
public interface Registry {
    void registService(RegistryInfo registryInfo);
    void unregister(RegistryInfo registryInfo);

    List<RegistryInfo> getServices(String serviceName, String version);

}
