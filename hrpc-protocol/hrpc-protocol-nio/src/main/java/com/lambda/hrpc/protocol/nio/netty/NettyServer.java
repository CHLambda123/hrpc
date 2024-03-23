package com.lambda.hrpc.protocol.nio.netty;

import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class NettyServer {
    private MessageToByteEncoder<Message> protocolEncoder;
    private ChannelInboundHandlerAdapter protocolHandler;
    private Map<String, Map<String, Object>> localServicesCache;

    public NettyServer(MessageToByteEncoder<Message> protocolEncoder,
                       ChannelInboundHandlerAdapter protocolHandler) {
        this.protocolEncoder = protocolEncoder;
        this.protocolHandler = protocolHandler;
    }

    public void start(Integer port) {
        try {
            NioEventLoopGroup parent = new NioEventLoopGroup(1);
            NioEventLoopGroup children = new NioEventLoopGroup(8);
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(parent, children);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            serverBootstrap.childHandler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline()
                            .addLast(protocolEncoder)
                            .addLast(new NettyDecoder(Invocation.AppInvocation.class))
                            .addLast(protocolHandler);
                }
            });
            log.info("netty server start on port: {}", port);
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        }
    }
}
