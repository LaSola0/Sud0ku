package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.visitor.TypeInferencer;

import java.util.List;


public class InstanceType extends Type {

    public Type classType;


    public InstanceType(@NotNull Type c) {
        table.setStateType(State.StateType.INSTANCE);
        table.addSuper(c.table);
        table.setPath(c.table.path);
        classType = c;
    }

    public InstanceType(@NotNull Type c, List<Type> args, TypeInferencer inferencer, Node call)
    {
        this(c);

        // call constructor
        Type initFunc = ta