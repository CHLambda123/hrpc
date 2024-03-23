package com.lambda.hrpc.registry.common.loadbalance;

import com.lambda.hrpc.common.spi.Spi;
import com.lambda.hrpc.common.registry.RegistryInfo;

import java.util.List;

@Spi
public interface LoadBalance {
    RegistryInfo selectOneService(List<RegistryInfo> services);
}
