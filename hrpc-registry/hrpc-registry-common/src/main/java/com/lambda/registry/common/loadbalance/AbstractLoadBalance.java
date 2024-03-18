package com.lambda.loadbalance;

import com.lambda.common.entity.RegistryInfo;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public RegistryInfo selectOneService(List<RegistryInfo> services) {
        if (services == null || services.isEmpty()) {
            return null;
        }
        if (services.size() == 1) {
            return services.get(0);
        }
        return doSelectOneService(services);
    }

    protected abstract RegistryInfo doSelectOneService(List<RegistryInfo> services);
    protected abstract String getName();
}
