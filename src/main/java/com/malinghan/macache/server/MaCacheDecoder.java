package com.malinghan.macache.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MaCacheDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        String msg = in.toString(StandardCharsets.UTF_8);


        if (msg.startsWith("*")) {
            // RESP array: first line is *N, need N*2+1 lines total
            String[] lines = msg.split("\\r\\n", -1);
            // lines[0] = "*N"
            int n;
            try {
                n = Integer.parseInt(lines[0].substring(1));
            } catch (NumberFormatException e) {
                in.resetReaderIndex();
                return;
            }
            int needed = 1 + n * 2; // *N line + N pairs of ($len, value)
            if (lines.length < needed + 1) {
                // +1 because split leaves an empty string at end when msg ends with \r\n
                in.resetReaderIndex();
                return;
            }
            in.readBytes(in.readableBytes()); // consume all
            out.add(msg);
        } else {
            // inline command: wait for \r\n
            if (!msg.contains("\r\n")) {
                in.resetReaderIndex();
                return;
            }
            in.readBytes(in.readableBytes());
            out.add(msg);
        }
    }
}
