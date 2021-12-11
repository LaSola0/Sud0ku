package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;

public class DictType extends Type {

    public Type keyType;
    public Type valueType;

    public DictType(Type key0, Type val0) {
        keyType = key0;
        valueType = val0;
        table.addSuper(Types.BaseDict.table);
        table.setPath(Types.BaseDict.table.path);
    }

    public void add(@NotNull Type key, @NotNull Type val) {
        keyType = UnionType.union(keyType, key);
        valueType = UnionType.union(valueType, val);
    }

    @NotNull
    public TupleType toTupleType(int n) {
        TupleType ret = new TupleType();
        for (int i = 0; i < n; i++) {
            ret.add(keyType);
        }
        return ret;
    }

    @Override
    public boolean typeEquals(Object other) {
        i