package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Call extends Node {

    public Node func;
    public List<Node> args;
    @Nullable
    public List<Keyword> k