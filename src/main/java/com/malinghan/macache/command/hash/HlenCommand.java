package com.malinghan.macache.command.hash;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class HlenCommand implements Command {
    @Override public String name() { return "HLEN"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.hlen(args[1]));
    }
}
