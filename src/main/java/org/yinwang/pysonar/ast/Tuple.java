package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Tuple extends Sequence {

    public Tuple(List<Node> elts, String file, int start, int end, int line, int col) {
        super(NodeType.TUPLE, elts, file, start, end, line, col);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Tuple:" + start + ":" + elts + ">";
    }

    @NotNull
    @Override
    public String toD