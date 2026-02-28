package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class InfoCommand implements Command {
    @Override
    public String name() { return "INFO"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.bulkString("# Server\r\nredis_version:7.0.0\r\nos:MaCache\r\n");
    }
}
