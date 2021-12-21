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
        this.eltT