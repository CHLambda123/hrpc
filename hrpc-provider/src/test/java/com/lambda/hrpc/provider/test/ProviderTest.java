package com.lambda.hrpc.provider.test;

import com.google.protobuf.Any;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.registry.Registry;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import com.lambda.hrpc.provider.MyInvocation;
import com.lambda.hrpc.provider.Provider;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ProviderTest {
    
    @Test
    public void providerTest() throws InterruptedException {
        // 创建provider并启动services
        Registry registry = ExtensionLoader.getExtensionLoader(Registry.class).getExtension("zookeeper", Map.of("zkUrl", "127.0.0.1:2181"));
        Provider provider = Provider.providerSingleTon(registry, "nio", null, "com.lambda.hrpc.provider");
        provider.startProvider();
        Thread.sleep(1000);
        // 返回provider提供的service，验证是否成功
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("nio", null);
        MyInvocation.MyAppInvocation lch = MyInvocation.MyAppInvocation.newBuilder().setName("lch").setAge(1).build();
        Invocation.AppInvocation invocation = Invocation.AppInvocation.newBuilder()
                .setServiceName("serviceName")
                .setVersion("v1")
                .setMethodName("test")
                .addAllParamTypes(List.of(lch.getClass().getName()))
                .addAllParams(List.of(Any.pack(lch))).build();
        String port = registry.getServices("serviceName", "v1").get(0).getPort();
        MyInvocation.MyAppInvocation res = protocol.executeRequest("127.0.0.1", Integer.valueOf(port), invocation, MyInvocation.MyAppInvocation.class);
        Assert.assertEquals(res.getName(), "lchhello");
        Assert.assertEquals(res.getAge(), 2);
    }
}
