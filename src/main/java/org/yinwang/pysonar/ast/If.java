package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class If extends Node {

    @NotNull
    public Node test;
    public Node body;
    public Node orelse;

    public If(@NotNull Node test, Node body, Node orelse, String file