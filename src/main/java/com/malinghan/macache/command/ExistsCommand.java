package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ExistsCommand implements Command {
    @Override
    public String name() { return "EXISTS"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.exists(getParams(args)));
    }
}
