package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.TypeStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TupleType extends Type {

    public List<Type> eltTypes;


    public TupleType() {
        this.eltTypes = new ArrayList<>();
        table.addSuper(Analyzer.self.builtins.BaseTuple.table);
        table.setPath(Analyzer.self.builtins.BaseTuple.table.path);
    }


    public TupleType(List<Type> eltTypes) {
        this();
        this.eltTypes = eltTypes;
    }


    public TupleType(Type elt0) {
        this();
        this.eltTypes.add(elt0);
    }


    public TupleType(Type elt0, Type elt1) {
        this();
        this.eltTypes.add(elt0);
        this.eltTypes.add(elt1);
    }


    public TupleType(Ty