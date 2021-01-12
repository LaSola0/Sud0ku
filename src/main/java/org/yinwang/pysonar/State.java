
package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.Types;
import org.yinwang.pysonar.types.UnionType;

import java.util.*;
import java.util.Map.Entry;


public class State {
    public enum StateType {
        CLASS,
        INSTANCE,
        FUNCTION,
        MODULE,
        GLOBAL,
        SCOPE
    }


    @NotNull
    public Map<String, Set<Binding>> table = new HashMap<>(0);
    @Nullable
    public State parent;      // all are non-null except global table
    @Nullable
    public State forwarding; // link to the closest non-class scope, for lifting functions out
    @Nullable
    public List<State> supers;
    @Nullable
    public Set<String> globalNames;
    public StateType stateType;
    public Type type;
    @NotNull
    public String path = "";


    public State(@Nullable State parent, StateType type) {
        this.parent = parent;
        this.stateType = type;

        if (type == StateType.CLASS) {
            this.forwarding = parent == null ? null : parent.getForwarding();
        } else {
            this.forwarding = this;
        }
    }


    public State(@NotNull State s) {
        this.table = new HashMap<>();
        this.table.putAll(s.table);
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.forwarding = s.forwarding;
        this.supers = s.supers;
        this.globalNames = s.globalNames;
        this.type = s.type;
        this.path = s.path;
    }


    // erase and overwrite this to s's contents
    public void overwrite(@NotNull State s) {
        this.table = s.table;
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.forwarding = s.forwarding;
        this.supers = s.supers;
        this.globalNames = s.globalNames;
        this.type = s.type;
        this.path = s.path;
    }
