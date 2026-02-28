package com.malinghan.macache.command.set;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class SmembersCommand implements Command {
    @Override public String name() { return "SMEMBERS"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.array(cache.smembers(args[1]));
    }
}
