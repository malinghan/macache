package com.malinghan.macache.command.zset;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ZcountCommand implements Command {
    @Override public String name() { return "ZCOUNT"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        double min = Double.parseDouble(args[2]);
        double max = Double.parseDouble(args[3]);
        return Reply.integer(cache.zcount(args[1], min, max));
    }
}
