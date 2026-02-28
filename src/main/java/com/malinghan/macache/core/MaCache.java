package com.malinghan.macache.core;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MaCache {

    private final Map<String, CacheEntry<?>> map = new HashMap<>();

    // ==================== String ====================

    public void set(String key, String value) {
        map.put(key, new CacheEntry<>(value));
    }

    public String get(String key) {
        CacheEntry<?> entry = map.get(key);
        return entry == null ? null : (String) entry.getValue();
    }

    public int strlen(String key) {
        String val = get(key);
        return val == null ? 0 : val.length();
    }

    public long del(String... keys) {
        long count = 0;
        for (String key : keys) {
            if (map.remove(key) != null) count++;
        }
        return count;
    }

    public long exists(String... keys) {
        long count = 0;
        for (String key : keys) {
            if (map.containsKey(key)) count++;
        }
        return count;
    }

    public long incr(String key) {
        String val = get(key);
        long num = val == null ? 0 : Long.parseLong(val);
        num++;
        set(key, String.valueOf(num));
        return num;
    }

    public long decr(String key) {
        String val = get(key);
        long num = val == null ? 0 : Long.parseLong(val);
        num--;
        set(key, String.valueOf(num));
        return num;
    }

    public void mset(String[] keys, String[] values) {
        for (int i = 0; i < keys.length; i++) {
            set(keys[i], values[i]);
        }
    }

    public List<String> mget(String... keys) {
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            result.add(get(key));
        }
        return result;
    }

    // ==================== List ====================

    @SuppressWarnings("unchecked")
    private LinkedList<String> getList(String key) {
        CacheEntry<?> entry = map.get(key);
        if (entry == null) return null;
        return (LinkedList<String>) entry.getValue();
    }

    private LinkedList<String> getOrCreateList(String key) {
        LinkedList<String> list = getList(key);
        if (list == null) {
            list = new LinkedList<>();
            map.put(key, new CacheEntry<>(list));
        }
        return list;
    }

    public long lpush(String key, String... values) {
        LinkedList<String> list = getOrCreateList(key);
        for (String v : values) list.addFirst(v);
        return list.size();
    }

    public long rpush(String key, String... values) {
        LinkedList<String> list = getOrCreateList(key);
        for (String v : values) list.addLast(v);
        return list.size();
    }

    public String lpop(String key) {
        LinkedList<String> list = getList(key);
        if (list == null || list.isEmpty()) return null;
        return list.removeFirst();
    }

    public String rpop(String key) {
        LinkedList<String> list = getList(key);
        if (list == null || list.isEmpty()) return null;
        return list.removeLast();
    }

    public long llen(String key) {
        LinkedList<String> list = getList(key);
        return list == null ? 0 : list.size();
    }

    public String lindex(String key, int index) {
        LinkedList<String> list = getList(key);
        if (list == null) return null;
        if (index < 0) index = list.size() + index;
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public List<String> lrange(String key, int start, int end) {
        LinkedList<String> list = getList(key);
        if (list == null) return Collections.emptyList();
        int size = list.size();
        if (start < 0) start = Math.max(0, size + start);
        if (end < 0) end = size + end;
        if (start > end || start >= size) return Collections.emptyList();
        end = Math.min(end, size - 1);
        return new ArrayList<>(list.subList(start, end + 1));
    }

    // ==================== Set ====================

    @SuppressWarnings("unchecked")
    private LinkedHashSet<String> getSet(String key) {
        CacheEntry<?> entry = map.get(key);
        if (entry == null) return null;
        return (LinkedHashSet<String>) entry.getValue();
    }

    private LinkedHashSet<String> getOrCreateSet(String key) {
        LinkedHashSet<String> set = getSet(key);
        if (set == null) {
            set = new LinkedHashSet<>();
            map.put(key, new CacheEntry<>(set));
        }
        return set;
    }

    public long sadd(String key, String... members) {
        LinkedHashSet<String> set = getOrCreateSet(key);
        long count = 0;
        for (String m : members) if (set.add(m)) count++;
        return count;
    }

    public List<String> smembers(String key) {
        LinkedHashSet<String> set = getSet(key);
        return set == null ? Collections.emptyList() : new ArrayList<>(set);
    }

    public long srem(String key, String... members) {
        LinkedHashSet<String> set = getSet(key);
        if (set == null) return 0;
        long count = 0;
        for (String m : members) if (set.remove(m)) count++;
        return count;
    }

    public long scard(String key) {
        LinkedHashSet<String> set = getSet(key);
        return set == null ? 0 : set.size();
    }

    public List<String> spop(String key, int count) {
        LinkedHashSet<String> set = getSet(key);
        if (set == null || set.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        Iterator<String> it = set.iterator();
        for (int i = 0; i < count && it.hasNext(); i++) {
            result.add(it.next());
            it.remove();
        }
        return result;
    }

    public long sismember(String key, String member) {
        LinkedHashSet<String> set = getSet(key);
        return (set != null && set.contains(member)) ? 1 : 0;
    }

    // ==================== Hash ====================

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> getHash(String key) {
        CacheEntry<?> entry = map.get(key);
        if (entry == null) return null;
        return (LinkedHashMap<String, String>) entry.getValue();
    }

    private LinkedHashMap<String, String> getOrCreateHash(String key) {
        LinkedHashMap<String, String> hash = getHash(key);
        if (hash == null) {
            hash = new LinkedHashMap<>();
            map.put(key, new CacheEntry<>(hash));
        }
        return hash;
    }

    public long hset(String key, String[] fields, String[] values) {
        LinkedHashMap<String, String> hash = getOrCreateHash(key);
        long count = 0;
        for (int i = 0; i < fields.length; i++) {
            if (!hash.containsKey(fields[i])) count++;
            hash.put(fields[i], values[i]);
        }
        return count;
    }

    public String hget(String key, String field) {
        LinkedHashMap<String, String> hash = getHash(key);
        return hash == null ? null : hash.get(field);
    }

    public List<String> hgetall(String key) {
        LinkedHashMap<String, String> hash = getHash(key);
        if (hash == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        hash.forEach((f, v) -> { result.add(f); result.add(v); });
        return result;
    }

    public long hlen(String key) {
        LinkedHashMap<String, String> hash = getHash(key);
        return hash == null ? 0 : hash.size();
    }

    public long hdel(String key, String... fields) {
        LinkedHashMap<String, String> hash = getHash(key);
        if (hash == null) return 0;
        long count = 0;
        for (String f : fields) if (hash.remove(f) != null) count++;
        return count;
    }

    public long hexists(String key, String field) {
        LinkedHashMap<String, String> hash = getHash(key);
        return (hash != null && hash.containsKey(field)) ? 1 : 0;
    }

    public List<String> hmget(String key, String... fields) {
        LinkedHashMap<String, String> hash = getHash(key);
        List<String> result = new ArrayList<>();
        for (String f : fields) result.add(hash == null ? null : hash.get(f));
        return result;
    }

    // ==================== ZSet ====================

    @SuppressWarnings("unchecked")
    private TreeSet<ZsetEntry> getZset(String key) {
        CacheEntry<?> entry = map.get(key);
        if (entry == null) return null;
        return (TreeSet<ZsetEntry>) entry.getValue();
    }

    private TreeSet<ZsetEntry> getOrCreateZset(String key) {
        TreeSet<ZsetEntry> zset = getZset(key);
        if (zset == null) {
            zset = new TreeSet<>();
            map.put(key, new CacheEntry<>(zset));
        }
        return zset;
    }

    public long zadd(String key, double score, String member) {
        TreeSet<ZsetEntry> zset = getOrCreateZset(key);
        // remove existing entry for this member if present
        zset.removeIf(e -> e.getValue().equals(member));
        return zset.add(new ZsetEntry(member, score)) ? 1 : 0;
    }

    public long zcard(String key) {
        TreeSet<ZsetEntry> zset = getZset(key);
        return zset == null ? 0 : zset.size();
    }

    public String zscore(String key, String member) {
        TreeSet<ZsetEntry> zset = getZset(key);
        if (zset == null) return null;
        return zset.stream()
                .filter(e -> e.getValue().equals(member))
                .findFirst()
                .map(e -> String.valueOf(e.getScore()))
                .orElse(null);
    }

    public long zrem(String key, String... members) {
        TreeSet<ZsetEntry> zset = getZset(key);
        if (zset == null) return 0;
        long count = 0;
        for (String m : members) {
            if (zset.removeIf(e -> e.getValue().equals(m))) count++;
        }
        return count;
    }

    public long zrank(String key, String member) {
        TreeSet<ZsetEntry> zset = getZset(key);
        if (zset == null) return -1;
        long rank = 0;
        for (ZsetEntry e : zset) {
            if (e.getValue().equals(member)) return rank;
            rank++;
        }
        return -1;
    }

    public long zcount(String key, double min, double max) {
        TreeSet<ZsetEntry> zset = getZset(key);
        if (zset == null) return 0;
        return zset.stream().filter(e -> e.getScore() >= min && e.getScore() <= max).count();
    }
}
