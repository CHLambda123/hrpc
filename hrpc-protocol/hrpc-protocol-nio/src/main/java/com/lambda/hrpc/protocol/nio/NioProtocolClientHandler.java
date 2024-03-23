package com.lambda.hrpc.protocol.nio;

import com.google.protobuf.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.atomic.AtomicReference;

public class NioProtocolClientHandler extends ChannelInboundHandlerAdapter {
    private AtomicReference<Message> sharedResult;
    public NioProtocolClientHandler(AtomicReference<Message> sharedResult) {
        this.sharedResult = sharedResult;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        sharedResult.set((Message) msg);
    }
}
