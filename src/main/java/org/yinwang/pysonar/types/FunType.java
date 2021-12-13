
package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.FunctionDef;
import org.yinwang.pysonar.hash.MyHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FunType extends Type {

    private static final int MAX_ARROWS = 10;

    @NotNull
    public Map<Type, Type> arrows = new MyHashMap<>();
    public FunctionDef func;
    @Nullable
    public ClassType cls = null;
    public State env;
    public List<Type> defaultTypes;       // types for default parameters (evaluated at def time)


    public FunType() {
    }


    public FunType(FunctionDef func, State env) {
        this.func = func;
        this.env = env;
    }


    public FunType(Type from, Type to) {
        addMapping(from, to);
        table.addSuper(Analyzer.self.builtins.BaseFunction.table);
        table.setPath(Analyzer.self.builtins.BaseFunction.table.path);
    }

