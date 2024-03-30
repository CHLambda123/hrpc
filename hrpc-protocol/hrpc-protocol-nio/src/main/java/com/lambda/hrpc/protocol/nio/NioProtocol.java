package com.lambda.hrpc.protocol.nio;

import com.google.protobuf.Message;
import com.lambda.hrpc.common.annotation.FieldName;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.common.protocol.Invocation;
import com.lambda.hrpc.common.protocol.Protocol;
import com.lambda.hrpc.protocol.nio.netty.NettyClientHandler;
import com.lambda.hrpc.protocol.nio.netty.NettyDecoder;
import com.lambda.hrpc.protocol.nio.netty.NettyEncoder;
import com.lambda.hrpc.protocol.nio.netty.NettyServer;
import com.lambda.hrpc.protocol.nio.netty.NettyServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class NioProtocol implements Protocol {
    private final MessageToByteEncoder<Message> protocolEncoder;
    private ChannelInboundHandlerAdapter protocolServerHandler;
    
    public NioProtocol(@FieldName("localServicesCache") Map<String, Map<String, Object>> localServicesCache) {
        this.protocolEncoder = new NettyEncoder();
        this.protocolServerHandler = new NettyServerHandler(localServicesCache);
    }
    
    public NioProtocol() {
        this.protocolEncoder = new NettyEncoder();
    }
    
    @Override
    public void startNewServer(Integer port) {
        String threadName = "nio-" + String.valueOf(port);
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (threadName.equals(thread.getName())) {
                return;
            }
        }
        Thread thread = new Thread(() -> {
            new NettyServer(protocolEncoder, protocolServerHandler).start(port);
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public <T> T executeRequest(String ip, Integer port, Invocation.AppInvocation invocation, Class<T> returnType) throws HrpcRuntimeException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            AtomicReference<Message> sharedResult = new AtomicReference<>();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(protocolEncoder)
                                    .addLast(new NettyDecoder(returnType))
                                    .addLast(new NettyClientHandler(sharedResult));
                        }
            });
            ChannelFuture future = bootstrap.connect(ip, port).sync();
            future.channel().writeAndFlush(invocation);
            if (!future.channel().closeFuture().await(1000)) {
                throw new HrpcRuntimeException("connect to server time out");
            }

            return (T)sharedResult.get();
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
