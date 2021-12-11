package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;

public class DictType extends Type {

    public Type keyType;
    public Type valueType;

    public DictType(Type key0, Type val0) {
        keyType = key0;
        valueType = val0;
        table.addSuper(Types.BaseDict.table);
        table.setPath(Types.BaseDict