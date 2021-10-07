package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Raise extends Node {

    public Node exceptionType;
    public Node inst;
    public Node traceback;

    public Raise(Node ex