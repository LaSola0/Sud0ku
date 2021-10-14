package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Return extends Node {

    public Node value;

    public Return(Node n, String file, in