package com.malinghan.macache.command.hash;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class HgetCommand implements Command {
    @Override public String name() { return "HGET"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String val = cache.hget(args[1], args[2]);
        return val == null ? Reply.nil() : Reply.bulkString(val);
    }
}
