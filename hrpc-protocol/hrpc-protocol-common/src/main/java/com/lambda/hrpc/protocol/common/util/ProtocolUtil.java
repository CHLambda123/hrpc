package com.lambda.hrpc.protocol.common.util;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.protocol.common.Invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProtocolUtil {
    public static Message bytesToMessage(byte[] bytes, Class<?> returnType) {
        if (!Message.class.isAssignableFrom(returnType)) {
            throw new HrpcRuntimeException("return type should be able to be serialized by protobuf");
        }
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return Any.parseFrom(bytes).unpack((Class<? extends Message>)returnType);
        } catch (InvalidProtocolBufferException e) {
            throw new HrpcRuntimeException(e);
        }
    }
    public static byte[] messageToBytes(Message message) {
        return Any.pack(message).toByteArray();
    }
    
    public static Object handleMethodInvoke(Invocation.AppInvocation invocation, Map<String, Map<String, Object>> localServicesCache) throws Exception {
        List<Class<? extends Message>> paramsTypeList = new ArrayList<>(10);
        List<String> typeStrs = invocation.getParamTypesList().asByteStringList().stream().map(k -> k.toStringUtf8()).collect(Collectors.toList());
        for (String typeStr : typeStrs) {
            Class<?> clazz = Class.forName(typeStr);
            if (Message.class.isAssignableFrom(clazz)) {
                paramsTypeList.add(clazz.asSubclass(Message.class));
            } else {
                throw new HrpcRuntimeException("exist obj that can't be serialized by protobuf");
            }
        }
        List<Any> paramsList = invocation.getParamsList();
        if (paramsList.size() != paramsTypeList.size()) {
            throw new HrpcRuntimeException("the count of params doesn't equal to that of paramTypes");
        }
        List<Message> messageList = new ArrayList<>(10);
        for (int i = 0; i < paramsList.size(); i++) {
            messageList.add(paramsList.get(i).unpack(paramsTypeList.get(i)));
        }
        Map<String, Object> tempMap = localServicesCache.get(invocation.getServiceName());
        if (tempMap == null) {
            throw new HrpcRuntimeException("can't find the service");
        }
        Object service = tempMap.get(invocation.getVersion());
        if (service == null) {
            throw new HrpcRuntimeException("can't find the service");
        }
        String methodName = invocation.getMethodName();
        Method method = service.getClass().getMethod(methodName, paramsTypeList.toArray(new Class<?>[0]));
        Object res = method.invoke(service, messageList.toArray());
        return res;
    }
}
