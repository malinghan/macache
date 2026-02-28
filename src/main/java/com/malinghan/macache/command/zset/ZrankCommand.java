package com.malinghan.macache.command.zset;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ZrankCommand implements Command {
    @Override public String name() { return "ZRANK"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        long rank = cache.zrank(args[1], args[2]);
        return rank == -1 ? Reply.nil() : Reply.integer(rank);
    }
}
