package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a keyword argument (name=value) in a function call.
 */
public class Keyword extends Node {

    public String arg;
    @NotNull
    public Node value;

    public Keyword(String arg, @NotNull Node value, String file, int start, int end, int line, int col) {
        super(NodeType.KEYWORD, file, 