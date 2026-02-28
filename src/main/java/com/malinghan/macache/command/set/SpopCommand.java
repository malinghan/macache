package com.malinghan.macache.command.set;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class SpopCommand implements Command {
    @Override public String name() { return "SPOP"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        int count = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        return Reply.array(cache.spop(args[1], count));
    }
}
