package com.lambda.hrpc.protocol.http;

import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.Servlet;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TomcatServer {
    private final String baseDir;
    private final String webAppDir;
    private final Integer port;

    private final Map<String, Servlet> servlets;

    public TomcatServer(String baseDir, String webAppDir, Integer port) {
        this.baseDir = baseDir;
        this.webAppDir = webAppDir;
        this.port = port;
        this.servlets = new HashMap<>(64);
    }

    public void addServlet(String pattern, Servlet servlet) {
        servlets.put(pattern, servlet);
    }

    public void start() {
        Tomcat tomcat = new Tomcat();

        // 设置Tomcat的基本目录
        tomcat.setBaseDir(baseDir);

        // 设置Connector监听的端口
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(port);
        tomcat.getService().addConnector(connector);

        // 创建Context
        File docBase = new File(webAppDir);
        Context ctx = tomcat.addContext("", docBase.getAbsolutePath());

        // 注册Servlet
        servlets.forEach((pattern, servlet) -> {
            Tomcat.addServlet(ctx, servlet.getClass().getName(), servlet);
            ctx.addServletMappingDecoded(pattern, servlet.getClass().getName());
        });

        // 启动Tomcat
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new HrpcRuntimeException(e);
        }
        tomcat.getServer().await();
    }
}
