package com.lambda.hrpc.protocol.http;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {
    private final Map<String, Map<String, Object>> localServicesCache;
    public DispatcherServlet(Map<String, Map<String, Object>> localServicesCache) {
        this.localServicesCache = localServicesCache;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        new HttpServerHandler().handler(req, resp, localServicesCache);
    }
}
