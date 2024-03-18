package com.lambda.loadbalance;

import com.lambda.common.spi.Spi;
import com.lambda.common.entity.RegistryInfo;

import java.util.List;

@Spi
public interface LoadBalance {
    RegistryInfo selectOneService(List<RegistryInfo> services);
}
