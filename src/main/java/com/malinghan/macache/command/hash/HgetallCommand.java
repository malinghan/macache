package com.malinghan.macache.command.hash;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class HgetallCommand implements Command {
    @Override public String name() { return "HGETALL"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.array(cache.hgetall(args[1]));
    }
}
