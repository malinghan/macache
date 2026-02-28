package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

import java.util.Arrays;

public class HelloCommand implements Command {
    @Override
    public String name() { return "HELLO"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        // If client requests RESP3 (HELLO 3), reject it so redis-cli falls back to RESP2
        if (args.length > 1 && "3".equals(args[1])) {
            return Reply.error("NOPROTO unsupported protocol version");
        }
        return Reply.array(Arrays.asList(
            "server", "redis",
            "version", "7.0.0",
            "proto", "2",
            "mode", "standalone",
            "role", "master"
        ));
    }
}
