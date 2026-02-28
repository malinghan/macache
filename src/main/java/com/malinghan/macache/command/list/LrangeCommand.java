package com.malinghan.macache.command.list;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class LrangeCommand implements Command {
    @Override public String name() { return "LRANGE"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.array(cache.lrange(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3])));
    }
}
