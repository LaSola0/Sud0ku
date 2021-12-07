
package org.yinwang.pysonar.hash;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MyHashMap<K, V>
        extends AbstractMap<K, V>
        implements Map<K, V>
{
    static final int DEFAULT_INITIAL_CAPACITY = 1;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    Entry<K, V>[] table;
    int size;
    int threshold;
    final float loadFactor;
    int modCount;

    private Set<Map.Entry<K, V>> entrySet = null;
    volatile Set<K> keySet = null;
    volatile Collection<V> values = null;

    HashFunction hashFunction;
    EqualFunction equalFunction;


    public MyHashMap(int initialCapacity, float loadFactor, HashFunction hashFunction, EqualFunction equalFunction) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }

        this.table = new Entry[0];
        this.loadFactor = loadFactor;
        this.hashFunction = hashFunction;
        this.equalFunction = equalFunction;
        threshold = initialCapacity;
    }


    public MyHashMap(int initialCapacity, HashFunction hashFunction, EqualFunction equalFunction) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, hashFunction, equalFunction);
    }


    public MyHashMap(HashFunction hashFunction, EqualFunction equalFunction) {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, hashFunction, equalFunction);
    }


    public MyHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, new GenericHashFunction(), new GenericEqualFunction());
    }


    public MyHashMap(Map<? extends K, ? extends V> m, HashFunction hashFunction, EqualFunction equalFunction) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY),
                DEFAULT_LOAD_FACTOR, hashFunction, equalFunction);
        putAll(m);
    }


    private static int roundup(int number) {
        if (number >= MAXIMUM_CAPACITY) {
            return MAXIMUM_CAPACITY;
        }
        int n = 1;
        while (n < number) {
            n = n << 1;
        }
        return n;
    }


    private void initTable(int size) {
        size = roundup(size);
        threshold = (int) Math.min(size * loadFactor, MAXIMUM_CAPACITY + 1);
        table = new Entry[size];
    }


    final int hash(Object k) {
        int h = hashFunction.hash(k);
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }


    static int slot(int h, int length) {
        return h & (length - 1);
    }


    @Override
    public int size() {
        return size;
    }


    @Override
    public boolean isEmpty() {
        return size == 0;
    }


    @Override
    public V get(@NotNull Object key) {
        Entry<K, V> entry = getEntry(key);
        return entry == null ? null : entry.getValue();
    }


    @Override
    public boolean containsKey(@NotNull Object key) {
        return getEntry(key) != null;
    }


    final Entry<K, V> getEntry(@NotNull Object key) {
        if (isEmpty()) {
            return null;
        }

        int h = hash(key);
        for (Entry<K, V> e = table[slot(h, table.length)];
             e != null;
             e = e.next)
        {
            if (equalFunction.equals(e.key, key)) {
                return e;
            }
        }
        return null;
    }


    @Override
    public V put(@NotNull K key, V value) {
        if (isEmpty()) {
            initTable(threshold);
        }
        int h = hash(key);
        int i = slot(h, table.length);
        for (Entry<K, V> e = table[i]; e != null; e = e.next) {
            if (equalFunction.equals(e.key, key)) {
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }

        modCount++;
        addEntry(h, key, value, i);
        return null;
    }


    void resize(int size) {
        if (size > MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
        } else {
            Entry[] table2 = new Entry[size];
            for (Entry<K, V> e : table) {
                while (e != null) {
                    Entry<K, V> next = e.next;
                    int i = slot(e.hash, size);
                    e.next = table2[i];
                    table2[i] = e;
                    e = next;
                }
            }
            table = table2;
            threshold = (int) Math.min(size * loadFactor, MAXIMUM_CAPACITY + 1);
        }
    }


    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }


    @Override
    public V remove(Object key) {
        Entry<K, V> e = removeEntry(key);
        return e == null ? null : e.value;
    }


    Entry<K, V> removeEntry(Object key) {
        if (isEmpty()) {
            return null;
        }
        int h = key == null ? 0 : hash(key);
        int i = slot(h, table.length);
        Entry<K, V> prev = table[i];
        Entry<K, V> e = prev;

        while (e != null) {
            Entry<K, V> next = e.next;
            if (equalFunction.equals(e.key, key)) {
                modCount++;
                size--;
                if (prev == e) {
                    table[i] = next;
                } else {
                    prev.next = next;
                }
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }


    Entry<K, V> removeMapping(Map.Entry entry) {
        Object key = entry.getKey();
        int hash = (key == null) ? 0 : hash(key);
        int i = slot(hash, table.length);
        Entry<K, V> prev = table[i];
        Entry<K, V> e = prev;

        while (e != null) {
            Entry<K, V> next = e.next;
            if (e.hash == hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e) {
                    table[i] = next;
                } else {
                    prev.next = next;
                }
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }


    @Override
    public void clear() {
        modCount++;
        Arrays.fill(table, null);
        size = 0;
    }


    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return containsNullValue();
        }

        Entry[] tab = table;
        for (Entry entry : tab) {
            for (Entry e = entry; e != null; e = e.next) {
                if (equalFunction.equals(value, e.value)) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean containsNullValue() {
        Entry[] tab = table;
        for (Entry entry : tab) {
            for (Entry e = entry; e != null; e = e.next) {
                if (e.value == null) {
                    return true;
                }
            }
        }
        return false;
    }


    static class Entry<K, V> implements Map.Entry<K, V> {
        final K key;
        V value;
        Entry<K, V> next;
        int hash;


        Entry(int h, K k, V v, Entry<K, V> n) {
            value = v;
            next = n;
            key = k;
            hash = h;
        }


        @Override
        public final K getKey() {
            return key;
        }


        @Override
        public final V getValue() {
            return value;
        }


        @Override
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }


        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || k1.equals(k2)) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }