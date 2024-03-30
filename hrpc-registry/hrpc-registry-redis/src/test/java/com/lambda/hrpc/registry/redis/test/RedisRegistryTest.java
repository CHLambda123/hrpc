package com.lambda.hrpc.registry.redis.test;

import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class RedisRegistryTest {
    @Test
    public void redisRegistryTest() throws InterruptedException {
        HashMap<String, Object> map = new HashMap<>();
        HashSet<String> sentinels = new HashSet<>();
        sentinels.add("192.168.238.131:26379");
        sentinels.add("192.168.238.131:26380");
        sentinels.add("192.168.238.131:26381");
        map.put("sentinels", sentinels);
        map.put("masterName", "testdb");
        map.put("password", "123456");
        Registry registry = ExtensionLoader.getExtensionLoader(Registry.class).getExtension("redis", map);
        RegistryInfo registryInfo = new RegistryInfo("serviceName", "v1", null, null, 1);
        registry.registService(registryInfo);
        Thread.sleep(1000);
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
