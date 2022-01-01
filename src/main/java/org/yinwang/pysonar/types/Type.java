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
    protected sta