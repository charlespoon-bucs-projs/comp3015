package org.e5200256.filetransfer;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

abstract class SelfHashPool<T> {
    private ConcurrentHashMap<Integer, T> m = new ConcurrentHashMap<>();

    String putNew(T t) {
        String hash = Integer.toHexString(t.hashCode()).intern();
        m.put(t.hashCode(), t);
        return hash;
    }

    public T peek(String hash) {
        int ihash = Integer.parseInt(hash, 16);
        return m.get(ihash);
    }

    public T remove(String hash) {
        int ihash = Integer.parseInt(hash, 16);
        return m.remove(ihash);
    }

    public T remove(T t) {
        return m.remove(t);
    }

    public int size() {
        return m.size();
    }

    public Collection<T> getAll() {
        return m.values();
    }
}
