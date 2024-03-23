package com.lambda.hrpc.protocol.nio;

import com.google.protobuf.Message;
import com.lambda.hrpc.common.exception.HrpcRuntimeException;
import com.lambda.hrpc.protocol.common.Invocation;
import com.lambda.hrpc.protocol.common.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
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
    private final ChannelInboundHandlerAdapter protocolServerHandler;
    public NioProtocol(Map<String, Map<String, Object>> localServicesCache) {
        this.protocolEncoder = new NioProtocolEncoder();
        this.protocolServerHandler = new NioProtocolServerHandler(localServicesCache);
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
            new NettyServer(protocolEncoder, protocolServerHandler).start(port);
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Message executeRequest(String ip, Integer port, Invocation.AppInvocation invocation, Class<?> returnType) throws HrpcRuntimeException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            AtomicReference<Message> sharedResult = new AtomicReference<>();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
//                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
//                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline()
                            .addLast(protocolEncoder)
                            .addLast(new NioProtocolDecoder(returnType))
                            .addLast(new NioProtocolClientHandler(sharedResult));
                }
            });
            ChannelFuture future = bootstrap.connect(ip, port).sync();
            future.channel().writeAndFlush(invocation);
            if (!future.channel().closeFuture().await(5000)) {
                throw new HrpcRuntimeException("connect to server time out");
            }

            return sharedResult.get();
        } catch (Exception e) {
            throw new HrpcRuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
