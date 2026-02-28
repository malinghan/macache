package com.malinghan.macache.command.list;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class LpushCommand implements Command {
    @Override public String name() { return "LPUSH"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        // args = ["LPUSH", key, v1, v2, ...]
        String key = args[1];
        String[] values = new String[args.length - 2];
        System.arraycopy(args, 2, values, 0, values.length);
        return Reply.integer(cache.lpush(key, values));
    }
}
