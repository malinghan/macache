package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class PersistCommand implements Command {
    @Override public String name() { return "PERSIST"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.persist(args[1]));
    }
}
