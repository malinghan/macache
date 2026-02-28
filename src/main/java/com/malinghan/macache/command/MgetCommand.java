package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class MgetCommand implements Command {
    @Override
    public String name() { return "MGET"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.array(cache.mget(getParams(args)));
    }
}
