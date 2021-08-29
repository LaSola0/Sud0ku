package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Pass extends Node {

    public Pass(String file, int start, int end, int line, int col) {
        super(NodeType.PASS, file, 