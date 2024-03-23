package com.lambda.hrpc.protocol.common;

import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.spi.Spi;

@Spi
public interface Protocol {
    void startNewServer(Integer port);

    Message executeRequest(String ip, Integer port, Invocation.AppInvocation invocation, Class<?> returnType) throws HrpcRuntimeException;
}
