package com.lambda.hrpc.provider.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HrpcService {
    String serviceName();
    String version();
    
    String host() default "";
    int weight() default 1;
}
