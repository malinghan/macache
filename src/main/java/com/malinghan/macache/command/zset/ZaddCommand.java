package com.malinghan.macache.command.zset;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class ZaddCommand implements Command {
    @Override public String name() { return "ZADD"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        // args = ["ZADD", key, score1, member1, score2, member2, ...]
        String key = args[1];
        long count = 0;
        for (int i = 2; i < args.length; i += 2) {
            double score = Double.parseDouble(args[i]);
            String member = args[i + 1];
            count += cache.zadd(key, score, member);
        }
        return Reply.integer(count);
    }
}
