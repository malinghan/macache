package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class PexpireCommand implements Command {
    @Override public String name() { return "PEXPIRE"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.pexpire(args[1], Long.parseLong(args[2])));
    }
}
