package com.lambda.hrpc.consumer.test;

import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.registry.RegistryInfo;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import com.lambda.hrpc.consumer.Consumer;
import com.lambda.hrpc.consumer.MyInvocation;
import com.lambda.hrpc.consumer.TestService;
import com.lambda.hrpc.consumer.TestServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ConsumerTest {
    private final String serviceName = "serviceName";
    private final String version = "v1";
    
    @Test
    public void consumerTest() throws InterruptedException {
        // 手动注册服务到注册中心
        Registry registry = ExtensionLoader.getExtensionLoader(Registry.class).getExtension("zookeeper", Map.of("zkUrl", "127.0.0.1:2181"));
        Map<String, Object> map = new HashMap<>();
        Map<String, Map<String, Object>> localServiceCache = new HashMap<>();
        localServiceCache.computeIfAbsent(serviceName, k -> new HashMap<>()).computeIfAbsent(version, k -> new TestServiceImpl());
        map.put("localServicesCache", localServiceCache);
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("nio", map);
        protocol.startNewServer(8081);
        registry.registService(new RegistryInfo(serviceName, version, "127.0.0.1", "8081", 1));
        Thread.sleep(1000);
        // 测试consumer
        Consumer consumer = Consumer.consumerSingleTon(registry, "nio", "com.lambda.hrpc.consumer");
        TestService service = consumer.getClient(TestService.class);
        MyInvocation.MyAppInvocation lch = MyInvocation.MyAppInvocation.newBuilder().setName("lch").setAge(1).build();
        MyInvocation.MyAppInvocation test = service.test(lch);
        Assert.assertEquals(test.getName(), "lchhello");
        Assert.assertEquals(test.getAge(), 2);
    }
    
}
