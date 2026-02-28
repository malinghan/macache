package com.malinghan.macache.command.zset;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ZscoreCommand implements Command {
    @Override public String name() { return "ZSCORE"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        String score = cache.zscore(args[1], args[2]);
        return score == null ? Reply.nil() : Reply.bulkString(score);
    }
}
