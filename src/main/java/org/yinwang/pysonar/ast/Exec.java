package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Exec extends Node {

    public Node body;
    public Node globals;
    public Node locals;

    p