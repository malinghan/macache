package com.malinghan.macache.command.list;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class RpushCommand implements Command {
    @Override public String name() { return "RPUSH"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String key = args[1];
        String[] values = new String[args.length - 2];
        System.arraycopy(args, 2, values, 0, values.length);
        return Reply.integer(cache.rpush(key, values));
    }
}
