package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class GetCommand implements Command {
    @Override
    public String name() { return "GET"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String val = cache.get(getKey(args));
        return val == null ? Reply.nil() : Reply.bulkString(val);
    }
}
