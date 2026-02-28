package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class PttlCommand implements Command {
    @Override public String name() { return "PTTL"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.pttl(args[1]));
    }
}
