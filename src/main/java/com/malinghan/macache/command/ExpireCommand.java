package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ExpireCommand implements Command {
    @Override public String name() { return "EXPIRE"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.expire(args[1], Long.parseLong(args[2])));
    }
}
