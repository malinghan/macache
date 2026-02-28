package com.malinghan.macache.core;

public class ZsetEntry implements Comparable<ZsetEntry> {
    private final String value;
    private final double score;

    public ZsetEntry(String value, double score) {
        this.value = value;
        this.score = score;
    }

    public String getValue() { return value; }
    public double getScore() { return score; }

    @Override
    public int compareTo(ZsetEntry o) {
        int cmp = Double.compare(this.score, o.score);
        return cmp != 0 ? cmp : this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZsetEntry)) return false;
        return value.equals(((ZsetEntry) o).value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }
}
