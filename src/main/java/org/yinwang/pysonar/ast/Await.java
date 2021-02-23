package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Await extends Node {

    public Node value;

    public Await(Node n, String file, int start, i