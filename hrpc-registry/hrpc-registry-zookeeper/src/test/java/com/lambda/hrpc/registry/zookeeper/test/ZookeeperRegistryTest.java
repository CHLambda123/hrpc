package com.lambda.hrpc.registry.zookeeper.test;

import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class ZookeeperRegistryTest {
    
    @Test
    public void zookeeperRegistryTest() throws InterruptedException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("zkUrl", "127.0.0.1:2181");
        Registry registry = ExtensionLoader.getExtensionLoader(Registry.class).getExtension("zookeeper", map);
        RegistryInfo registryInfo = new RegistryInfo("serviceName", "v1", null, null, 1);
        registry.registService(registryInfo);
        Thread.sleep(10000);
        List<RegistryInfo> services = registry.getServices("serviceName", "v1");
        Assert.assertEquals(services.size(), 1);
        Assert.assertEquals(services.get(0).getServiceName(), registryInfo.getServiceName());
        Assert.assertEquals(services.get(0).getVersion(), registryInfo.getVersion());
        Assert.assertEquals(services.get(0).getWeight(), registryInfo.getWeight());
        registry.unregistService(registryInfo);
        Thread.sleep(1000);
        services = registry.getServices("serviceName", "V1");
        Assert.assertEquals(services.size(), 0);
    }
}
