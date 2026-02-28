package com.malinghan.macache.command.set;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class SremCommand implements Command {
    @Override public String name() { return "SREM"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String key = args[1];
        String[] members = new String[args.length - 2];
        System.arraycopy(args, 2, members, 0, members.length);
        return Reply.integer(cache.srem(key, members));
    }
}
