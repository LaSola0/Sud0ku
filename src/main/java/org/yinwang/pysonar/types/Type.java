package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.TypeStack;
import org.yinwang.pysonar.$;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class Type {

    @NotNull
    public State table = new State(null, State.StateType.SCOPE);
    public String file = null;
    @NotNull
    protected static TypeStack typeStack = new TypeStack();


    public Type() {
    }

    @Override
    public boolean equals(Object other) {
        return typeEquals(other);
    }

    public abstract boolean typeEquals(Object other);

    public void setTable(@NotNull State table) {
        this.table = table;
    }


    public void setFile(String file) {
        this.file = file;
    }


    public boolean isNumType() {
	    return this == Types.IntInstance || this == Types.FloatInstance;
    }


    public boolean isUnknownType() {
        return this == Types.UNKNOWN;
    }


    @NotNull
    public ModuleType asModuleType() {
        if (this instanceof UnionType) {
            for (Type t : ((UnionType) this).types) {
                if (t instanceof ModuleType) {
                    return t.asModuleType();
                }
            }
     