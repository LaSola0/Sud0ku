package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Handler extends Node {

    public List<Node> exceptions;
    public Node binder;
    public Block body;

    public Handler(List<Node> exceptions, Node binder, Block body, String file, int sta