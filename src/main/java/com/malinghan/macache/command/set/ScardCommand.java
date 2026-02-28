package com.malinghan.macache.command.set;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ScardCommand implements Command {
    @Override public String name() { return "SCARD"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.scard(args[1]));
    }
}
