package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class UnionType extends Type {

    public Set<Type> types;


    public UnionType() {
        this.types = new HashSet<>();
    }


    public UnionType(@NotNull Type... initialTypes) {
        this();
        for (Type nt : initialTypes) {
            addType(nt);
        }
    }


    public boolean isEmpty() {
        return types.isEmpty();
    }


    /**
     * Returns true if t1 == t2 or t1 is a un