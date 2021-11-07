package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Try extends Node {

    public List<Handler> handlers;
    public Block body;
    public Block orelse;
    public Block finalbody;

    public Try(List<Handler> handlers, Block body, Block orelse, Block finalbody,
        String fil