package com.malinghan.macache.command.zset;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ZcardCommand implements Command {
    @Override public String name() { return "ZCARD"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.zcard(args[1]));
    }
}
