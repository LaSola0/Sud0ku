package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Attribute extends Node {

    @NotNull
    public Node target;
    @NotNull
    public Name attr;

    public Attribute(@NotNull Node target, @NotNull Name attr, String file, int start, int end, int line,