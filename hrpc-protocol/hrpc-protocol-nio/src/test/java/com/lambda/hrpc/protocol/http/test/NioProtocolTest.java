package com.lambda.hrpc.protocol.http.test;

import com.google.protobuf.Any;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.common.spi.ExtensionLoader;
import com.lambda.hrpc.protocol.http.MyInvocation;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NioProtocolTest {
    private String protocolType = "nio";
    private String serviceName = "serviceName";
    private String version = "v1";
    @Test
    public void nioProtocolServerTest() throws InterruptedException {
        Map<String, Object> map = new HashMap<>();
        Map<String, Map<String, Object>> localServiceCache = new HashMap();
        localServiceCache.computeIfAbsent(serviceName, k -> new HashMap<>()).computeIfAbsent(version, k -> new NioProtocolTest());
        map.put("localServicesCache", localServiceCache);
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolType, map);
        protocol.startNewServer(8081);
        Thread.sleep(1000);
        MyInvocation.MyAppInvocation myAppInvocation = nioProtocolClientTest();
        Assert.assertEquals(myAppInvocation.getName(), "lchhello");
        Assert.assertEquals(myAppInvocation.getAge(), 2);
    }

    MyInvocation.MyAppInvocation nioProtocolClientTest() {
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolType, new HashMap<>());
        MyInvocation.MyAppInvocation mi = MyInvocation.MyAppInvocation.newBuilder().setName("lch").setAge(1).build();
        List<String> typeNames = List.of(mi.getClass().getName());
        Invocation.AppInvocation invocation = Invocation.AppInvocation.newBuilder()
                .setServiceName(serviceName)
                .setVersion(version)
                .setMethodName("test")
                .addAllParamTypes(typeNames)
                .addAllParams(List.of(Any.pack(mi))).build();

        return protocol.executeRequest("127.0.0.1", 8081, invocation, MyInvocation.MyAppInvocation.class);
    }

    public MyInvocation.MyAppInvocation test(MyInvocation.MyAppInvocation invocation) {
        return MyInvocation.MyAppInvocation.newBuilder().setName(invocation.getName() + "hello")
                .setAge(invocation.getAge() + 1).build();
    }
}
