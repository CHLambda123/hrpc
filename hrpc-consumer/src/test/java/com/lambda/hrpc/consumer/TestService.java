package com.lambda.hrpc.consumer;

import com.lambda.hrpc.consumer.proxy.HrpcClient;

@HrpcClient(serviceName = "serviceName", version = "v1")
public interface TestService {
    MyInvocation.MyAppInvocation test(MyInvocation.MyAppInvocation str);
}
