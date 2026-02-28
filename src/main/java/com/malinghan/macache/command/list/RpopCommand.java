package com.malinghan.macache.command.list;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class RpopCommand implements Command {
    @Override public String name() { return "RPOP"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String val = cache.rpop(args[1]);
        return val == null ? Reply.nil() : Reply.bulkString(val);
    }
}
