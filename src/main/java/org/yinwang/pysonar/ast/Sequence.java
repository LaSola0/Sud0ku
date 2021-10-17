package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class Sequence extends Node {

    @NotNull
    public List<Node> elts;

    public Sequence(NodeType nodeType, @NotNull Li