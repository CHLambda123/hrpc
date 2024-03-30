package com.lambda.hrpc.consumer.loadbalance;

import com.lambda.hrpc.common.registry.RegistryInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SimpleLoadBalance implements LoadBalance{
    @Override
    public RegistryInfo selectOneService(List<RegistryInfo> services) {
        services = services.stream().filter(s -> s.getWeight() > 0).collect(Collectors.toList());
        if (services.size() == 1) {
            return services.get(0);
        }
        int allWeight = 0;
        for (RegistryInfo service : services) {
            allWeight += service.getWeight();
        }
        int[] weights = new int[services.size()];
        for (int i = 0; i < weights.length; i++) {
            if (i == 0) {
                weights[i] = services.get(i).getWeight();
            } else {
                weights[i] = weights[i-1] + services.get(i).getWeight();
            }
        }
        int chooseWeight = new Random().nextInt(allWeight);
        // 取插入点的右边界
        int index = Math.abs(Arrays.binarySearch(weights, chooseWeight) + 1);
        return services.get(index);
    }
    
}
