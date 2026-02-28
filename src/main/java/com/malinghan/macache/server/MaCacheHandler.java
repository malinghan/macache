package com.malinghan.macache.server;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.command.Commands;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.core.WrongTypeException;
import com.malinghan.macache.reply.Reply;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ChannelHandler.Sharable
public class MaCacheHandler extends SimpleChannelInboundHandler<String> {

    @Autowired
    private MaCache cache;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        String[] lines = msg.split("\\r\\n");
        if (lines.length == 0 || lines[0].isEmpty()) return;

        String[] args;
        if (lines[0].startsWith("*")) {
            args = parseResp(lines);
        } else {
            args = lines[0].trim().split("\\s+");
        }

        if (args == null || args.length == 0) {
            ctx.writeAndFlush("-ERR empty command\r\n");
            return;
        }

        String cmdName = args[0];
        Command command = Commands.get(cmdName);
        if (command == null) {
            ctx.writeAndFlush("-ERR unknown command '" + cmdName + "'\r\n");
            return;
        }

        try {
            Reply<?> reply = command.exec(cache, args);
            ctx.writeAndFlush(encode(reply));
        } catch (WrongTypeException e) {
            ctx.writeAndFlush("-" + e.getMessage() + "\r\n");
        } catch (Exception e) {
            ctx.writeAndFlush("-ERR " + e.getMessage() + "\r\n");
        }
    }

    /**
     * Parse RESP array into a flat String[] of just the values (no length prefixes).
     * Input lines example for "SET foo bar":
     *   lines[0]="*3", lines[1]="$3", lines[2]="SET", lines[3]="$3", lines[4]="foo", lines[5]="$3", lines[6]="bar"
     * Output: ["SET", "foo", "bar"]
     */
    private String[] parseResp(String[] lines) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("$") || line.startsWith("*") || line.startsWith(":")) {
                continue; // skip length/type markers
            }
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result.toArray(new String[0]);
    }

    private String encode(Reply<?> reply) {
        return switch (reply.getType()) {
            case SIMPLE_STRING -> "+" + reply.getData() + "\r\n";
            case ERROR -> "-" + reply.getData() + "\r\n";
            case INT -> ":" + reply.getData() + "\r\n";
            case BULK_STRING -> {
                String data = (String) reply.getData();
                if (data == null) yield "$-1\r\n";
                yield "$" + data.length() + "\r\n" + data + "\r\n";
            }
            case ARRAY -> {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) reply.getData();
                if (list == null) yield "*-1\r\n";
                StringBuilder sb = new StringBuilder("*").append(list.size()).append("\r\n");
                for (String item : list) {
                    if (item == null) {
                        sb.append("$-1\r\n");
                    } else {
                        sb.append("$").append(item.length()).append("\r\n").append(item).append("\r\n");
                    }
                }
                yield sb.toString();
            }
        };
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
