package com.lambda.hrpc.provider;

import com.lambda.hrpc.provider.annotation.HrpcService;

@HrpcService(serviceName = "serviceName", version = "v1", weight = 1)
public class TestService {
    public MyInvocation.MyAppInvocation test(MyInvocation.MyAppInvocation invocation) {
        return MyInvocation.MyAppInvocation.newBuilder()
                .setName(invocation.getName()+"hello")
                .setAge(invocation.getAge()+1)
                .build();
    }
}
