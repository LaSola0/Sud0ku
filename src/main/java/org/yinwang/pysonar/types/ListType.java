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
    public List<Object> values = new ArrayList<>();


    public ListType() {
        this(Types.UNKNOWN);
    }


    public ListType(Type elt0) {
        eltType = elt0;
        table.addSuper(Analyzer.self.builtins.BaseList.table);
        table.setPath(Analyzer.self.builtins.BaseList.table.path);
    }


    public void setElementType(Type eltType) {
        this.eltType = eltType;
    }


    public void add(@NotNull Type another) {
        eltType = UnionType.union(eltType, another);
        positional.add(another);
    }


    pub