package org.yinwang.pysonar.hash;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


public class MyHashSet<E>
        extends AbstractSet<E>
        implements Set<E>
{
    private transient MyHashMap<E, Object> map;
    private static final Object PRESENT = new Object();


    public MyHashSet(HashFunction hashFunction, EqualFunction equalFunction) {
        map = new MyHashMap<>(hashFunction, equalFunction);
    }


    public MyHashSet(Collection<? extends E> c, HashFunction hashFunction, EqualFunction equalFunction) {
        map = new MyHashMap<>(Math.max((int) (c.size() / .75f) + 1, 16), hashFunction, equalFunction);
        addAll(c);
    }


    public MyHashSet(int initialCapacity, float loadFactor, HashFunction hashFun