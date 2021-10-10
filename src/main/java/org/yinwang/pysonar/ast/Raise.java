package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Raise extends Node {

    public Node exceptionType;
    public Node inst;
    public Node traceback;

    public Raise(Node exceptionType, Node inst, Node traceback, String file, int start, int end, int line, int col) {
        super(NodeType.RAISE, file, start, end, line, col);
        this.ex