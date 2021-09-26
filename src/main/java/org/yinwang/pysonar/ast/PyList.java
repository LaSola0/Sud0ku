package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PyList extends Sequence {

    public PyList(@NotNull List<Node> elts, String file, int start, i