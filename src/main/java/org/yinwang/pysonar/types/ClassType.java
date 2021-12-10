package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.visitor.TypeInferencer;

import java.util.ArrayList;
import java.util.List;

public class ClassType extends Type {

    public String name;
    public Type superclass;
    private InstanceType instance;

    public ClassType(@NotNull String name, @