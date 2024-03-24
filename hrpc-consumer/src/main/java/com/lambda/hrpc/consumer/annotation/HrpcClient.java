package com.lambda.hrpc.consumer.annotation;

public @interface HrpcClient {
    String serviceName();
    String version();
}
