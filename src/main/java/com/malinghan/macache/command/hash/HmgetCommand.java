package com.malinghan.macache.command.hash;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class HmgetCommand implements Command {
    @Override public String name() { return "HMGET"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String key = args[1];
        String[] fields = new String[args.length - 2];
        System.arraycopy(args, 2, fields, 0, fields.length);
        return Reply.array(cache.hmget(key, fields));
    }
}
