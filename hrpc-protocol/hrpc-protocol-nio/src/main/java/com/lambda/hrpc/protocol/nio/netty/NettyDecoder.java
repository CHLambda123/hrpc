package com.lambda.hrpc.protocol.nio.netty;

import com.google.protobuf.Message;
import com.lambda.hrpc.common.protocol.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class NettyDecoder extends ByteToMessageDecoder {
    private int length = 0;
    private final Class<?> decoderType;
    
    public NettyDecoder(Class<?> decoderType) {
        this.decoderType = decoderType;
    }
    
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> outList) {
        if (byteBuf.readableBytes() >= Integer.SIZE / 8) {
            if (length == 0) {
                length = byteBuf.readInt();
            }
            if (byteBuf.readableBytes() < length) {
                return;
            }
            byte[] content = new byte[length];
            byteBuf.readBytes(content);
            Message message = ProtocolUtil.bytesToMessage(content, decoderType);
            outList.add(message);
            length = 0;
        }
    }
}
