package com.malinghan.macache.command;

import com.malinghan.macache.core.CacheEntry;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.core.ZsetEntry;
import com.malinghan.macache.persistence.AofWriter;
import com.malinghan.macache.reply.Reply;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BgrewriteaofCommand implements Command {

    @Autowired
    private AofWriter aofWriter;

    @PostConstruct
    public void register() {
        Commands.register(this);
    }

    @Override
    public String name() { return "BGREWRITEAOF"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        new Thread(() -> rewrite(cache)).start();
        return Reply.bulkString("Background append only file rewriting started");
    }

    @SuppressWarnings("unchecked")
    private void rewrite(MaCache cache) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CacheEntry<?>> entry : cache.getAll().entrySet()) {
            String key = entry.getKey();
            CacheEntry<?> ce = entry.getValue();
            if (ce.isExpired()) continue;
            Object val = ce.getValue();

            if (val instanceof String s) {
                sb.append(resp("SET", key, s));
            } else if (val instanceof LinkedList<?> list) {
                for (Object item : (LinkedList<String>) list) {
                    sb.append(resp("RPUSH", key, (String) item));
                }
            } else if (val instanceof LinkedHashSet<?> set) {
                for (Object item : (LinkedHashSet<String>) set) {
                    sb.append(resp("SADD", key, (String) item));
                }
            } else if (val instanceof LinkedHashMap<?, ?> hash) {
                for (Map.Entry<?, ?> e : ((LinkedHashMap<String, String>) hash).entrySet()) {
                    sb.append(resp("HSET", key, (String) e.getKey(), (String) e.getValue()));
                }
            } else if (val instanceof TreeSet<?> zset) {
                for (ZsetEntry ze : (TreeSet<ZsetEntry>) zset) {
                    sb.append(resp("ZADD", key, String.valueOf(ze.getScore()), ze.getValue()));
                }
            }

            if (ce.getExpireAt() > 0) {
                sb.append(resp("PEXPIRE", key, String.valueOf(ce.getExpireAt() - System.currentTimeMillis())));
            }
        }
        aofWriter.rewrite(sb.toString());
    }

    private String resp(String... parts) {
        StringBuilder sb = new StringBuilder("*").append(parts.length).append("\r\n");
        for (String p : parts) {
            sb.append("$").append(p.length()).append("\r\n").append(p).append("\r\n");
        }
        return sb.toString();
    }
}
