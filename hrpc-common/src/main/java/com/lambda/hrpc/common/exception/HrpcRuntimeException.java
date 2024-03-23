package com.lambda.hrpc.common.exception;

public class HrpcRuntimeException extends RuntimeException{
    public HrpcRuntimeException(Exception e) {
        super(e);
    }
    public HrpcRuntimeException(String message) {
        super(message);
    }
}
