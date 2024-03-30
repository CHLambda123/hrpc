package com.lambda.hrpc.consumer;

public class TestServiceImpl implements TestService{
    @Override
    public MyInvocation.MyAppInvocation test(MyInvocation.MyAppInvocation str) {
        return MyInvocation.MyAppInvocation.newBuilder().setName(str.getName() + "hello").setAge(str.getAge() + 1).build();
    }
}
