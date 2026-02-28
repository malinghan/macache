package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class PingCommand implements Command {
    @Override
    public String name() { return "PING"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.bulkString("PONG");
    }
}
