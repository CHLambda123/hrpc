package com.lambda.hrpc.provider.entity;

import lombok.Data;

@Data
public class Service {
    private String serviceName;
    private String version;
    private Object object;
}
