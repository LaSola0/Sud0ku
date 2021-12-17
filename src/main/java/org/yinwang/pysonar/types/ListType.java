package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;

import java.util.ArrayList;
import java.util.List;


public class ListType extends Type {

    public Type eltType;
    @NotNull
    public List<Type> positional = new ArrayList<>();
    @NotNull
    public List<Object> values 