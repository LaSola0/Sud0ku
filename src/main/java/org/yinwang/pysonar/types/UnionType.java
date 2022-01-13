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
     * Returns true if t1 == t2 or t1 is a union type that contains t2.
     */
    public static boolean contains(Type t1, Type t2) {
        if (t1 instanceof UnionType) {
            return ((UnionType) t1).contains(t2);
        } else {
            return t1.equals(t2);
        }
    }


    public static Type remove(Type t1, Type t2) {
        if (t1 instanceof UnionType) {
            Set<Type> types = new HashSet<>(((UnionType) t1).types);
            types.remove(t2);
            return UnionType.newUnion(types);
        } else if (t1 != Types.CONT && t1 == t2) {
            return Types.UNKNOWN;
        } else {
            return t1;
        }
    }


    @NotNull
    public static Type newUnion(@NotNull Collection<Type> types) {
        Type t = Types.UNKNOWN;
        for (Type nt : types) {
            t = union(t, nt);
        }
        return t;
    }


    public void setTypes(Set<Type> types) {
        this.types = types;
    }


    public void addType(@NotNull Type t) {
        if (t instanceof UnionType) {
            types.addAll(((UnionType) t).types);
        } else {
            types.add(t);
        }
    }


    public boolean contains(Type t) {
        return types.contains(t);
    }


    // take a union of two types
    // with preference: other > None > Cont > unknown
    @NotNull
    public static Type union(@NotNull Type u, @NotNull Type v) {
        if (u.equals(v)) {
            return u;
        } else if (u != Types.UNKNOWN && v == Types.UNKNOWN) {
            return u;
        } else if (v != Types.UNKNOWN && u == Types.UNKNOWN) {
            return v;
        } else if (u != Types.NoneInstance && v == Types.NoneInstance) {
            return u;
        } else if (v != Types.NoneInstance && u == Types.NoneInstance) {
            return v;
        } else if (u instanceof TupleType && v instanceof TupleType &&
                   ((TupleType)