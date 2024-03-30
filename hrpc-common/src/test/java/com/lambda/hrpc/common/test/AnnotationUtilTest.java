package com.lambda.hrpc.common.test;

import com.lambda.hrpc.common.TestEntity;
import com.lambda.hrpc.common.annotation.AnnotationUtil;
import com.lambda.hrpc.common.annotation.EnableHrpc;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AnnotationUtilTest {
    @Test
    public void annotationUtilTest() {
        List<Class<?>> classes = AnnotationUtil.scanAnnotation("com.lambda.hrpc.common", EnableHrpc.class);
        Assert.assertEquals(classes.size(), 1);
        Assert.assertEquals(classes.get(0), TestEntity.class);
    }
}
