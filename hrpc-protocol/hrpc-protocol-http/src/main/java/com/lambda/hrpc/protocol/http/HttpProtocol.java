package com.lambda.hrpc.protocol.http;

import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.protocol.common.Invocation;
import com.lambda.hrpc.protocol.common.Protocol;
import com.lambda.hrpc.protocol.common.util.ProtocolUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URI;
import java.util.Map;

public class HttpProtocol implements Protocol {
    private final Map<String, Map<String, Object>> localServicesCache;
    private final String baseDir;
    private final String webAppDir;

    public HttpProtocol(Map<String, Map<String, Object>> localServicesCache, String baseDir, String webAppDir) {
        this.localServicesCache = localServicesCache;
        this.baseDir = baseDir;
        this.webAppDir = webAppDir;
    }

    @Override
    public void startNewServer(Integer port) {
        String threadName = String.valueOf(port);
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (threadName.equals(thread.getName())) {
                return;
            }
        }
        Thread thread = new Thread(() -> {
            TomcatServer tomcatServer = new TomcatServer(baseDir, webAppDir, port);
            tomcatServer.addServlet("/", new DispatcherServlet(localServicesCache));
            tomcatServer.start();
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Message executeRequest(String ip, Integer port, Invocation.AppInvocation invocation, Class<?> returnType) throws HrpcRuntimeException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost();
            httpPost.setEntity(new ByteArrayEntity(invocation.toByteArray()));
            httpPost.setURI(URI.create("http://" + ip + ":" + port));
            CloseableHttpResponse httpResponse = client.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new HrpcRuntimeException("http error code: " + statusCode +
                        "\nentity: " + httpResponse.getEntity());
            }
            byte[] bytes = httpResponse.getEntity().getContent().readAllBytes();
            return ProtocolUtil.bytesToMessage(bytes, returnType);
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
    }
}
