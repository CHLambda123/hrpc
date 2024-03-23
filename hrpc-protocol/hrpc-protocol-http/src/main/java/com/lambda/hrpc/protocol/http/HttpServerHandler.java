package com.lambda.hrpc.protocol.http;


import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.util.ProtocolUtil;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


public class HttpServerHandler {

    public void handler(HttpServletRequest request, HttpServletResponse response, Map<String, Map<String, Object>> localServicesCache) {
        try {
            ServletInputStream in = request.getInputStream();
            byte[] bytes = in.readAllBytes();
            Invocation.AppInvocation invocation = Invocation.AppInvocation.parseFrom(bytes);
            Object res = ProtocolUtil.handleMethodInvoke(invocation, localServicesCache);
            if (res == null) {
                response.getOutputStream().write(new byte[]{});
            } else {
                if (!Message.class.isAssignableFrom(res.getClass())) {
                    throw new HrpcRuntimeException("the result can't be serialized by protobuf");
                }
                response.getOutputStream().write(ProtocolUtil.messageToBytes((Message) res));
            }
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
    }

}
