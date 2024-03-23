package com.lambda.hrpc.common.spi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Spi {
    String value() default "";
}
