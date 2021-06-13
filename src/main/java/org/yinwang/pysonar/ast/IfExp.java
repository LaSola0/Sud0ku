package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class IfExp extends Node {

    public Node test;
    public Node body;
    public Node orelse;

    public IfExp(Node test, Node body, Node orelse, String file, int start, int end, int line, int col) {
        super(NodeType.IFEXP, file, start, e