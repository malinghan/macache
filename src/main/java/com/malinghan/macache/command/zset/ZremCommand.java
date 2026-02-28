package com.malinghan.macache.command.zset;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ZremCommand implements Command {
    @Override public String name() { return "ZREM"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String key = args[1];
        String[] members = new String[args.length - 2];
        System.arraycopy(args, 2, members, 0, members.length);
        return Reply.integer(cache.zrem(key, members));
    }
}
