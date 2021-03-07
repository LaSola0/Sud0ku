package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Block extends Node {

    @NotNull
    public List<Node> seq;

    public Block(@NotNull List<Node> seq, String file, int star