package com.lambda.hrpc.protocol.nio.netty;

import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.util.ProtocolUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;

@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<Invocation.AppInvocation> {
    private final Map<String, Map<String, Object>> localServicesCache;
    public NettyServerHandler(Map<String, Map<String, Object>> localServicesCache) {
        this.localServicesCache = new HashMap<>();
        this.localServicesCache.putAll(localServicesCache);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Invocation.AppInvocation invocation) throws Exception {
        if (invocation == null) {
            channelHandlerContext.writeAndFlush(null);
            channelHandlerContext.close();
            return;
        }
        Object res = ProtocolUtil.handleMethodInvoke(invocation, localServicesCache);
        if (res == null) {
            channelHandlerContext.writeAndFlush(new byte[]{});
        } else {
            if (!Message.class.isAssignableFrom(res.getClass())) {
                throw new HrpcRuntimeException("the result can't be serialized by protobuf");
            }
            channelHandlerContext.writeAndFlush(res);
            channelHandlerContext.close();
        }
    }
}
