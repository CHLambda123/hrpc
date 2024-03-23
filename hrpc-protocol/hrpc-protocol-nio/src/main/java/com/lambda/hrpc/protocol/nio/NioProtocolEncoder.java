package com.lambda.hrpc.protocol.nio;

import com.google.protobuf.Message;
import com.lambda.hrpc.protocol.common.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class NioProtocolEncoder extends MessageToByteEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message message, ByteBuf byteBuf) throws Exception {
        byte[] bytes = ProtocolUtil.messageToBytes(message);
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);
    }
}
