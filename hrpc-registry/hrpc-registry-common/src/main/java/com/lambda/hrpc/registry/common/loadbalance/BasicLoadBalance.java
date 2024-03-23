package com.lambda.hrpc.registry.common.loadbalance;

import com.lambda.hrpc.common.entity.RegistryInfo;

import java.util.List;

public class BasicLoadBalance extends AbstractLoadBalance {

    @Override
    public RegistryInfo doSelectOneService(List<RegistryInfo> services) {
        return null;
    }

    @Override
    public String getName() {
        return "basic";
    }
}
