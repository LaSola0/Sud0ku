package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;

public class DictType extends Type {

    public Type keyType;
    public Type valueType;

    public DictType(Type key