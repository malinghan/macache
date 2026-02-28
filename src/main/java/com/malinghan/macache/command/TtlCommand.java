package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class TtlCommand implements Command {
    @Override public String name() { return "TTL"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.ttl(args[1]));
    }
}
