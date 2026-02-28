package com.malinghan.macache.command.list;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class LindexCommand implements Command {
    @Override public String name() { return "LINDEX"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String val = cache.lindex(args[1], Integer.parseInt(args[2]));
        return val == null ? Reply.nil() : Reply.bulkString(val);
    }
}
