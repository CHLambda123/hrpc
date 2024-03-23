package com.lambda.hrpc.common.protocol;

import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.spi.Spi;

@Spi
public interface Protocol {
    void startNewServer(Integer port);

    <T> T executeRequest(String ip, Integer port, Invocation.AppInvocation invocation, Class<T> returnType) throws HrpcRuntimeException;
}
