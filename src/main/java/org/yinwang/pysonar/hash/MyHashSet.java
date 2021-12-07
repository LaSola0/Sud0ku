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
    private st