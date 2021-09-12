package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public class PyInt extends Node {

    public BigInteger value;

    public PyInt(String s, String file, int start, int end, int line, int col) {
        super(NodeType.PYINT, file, start, end, line, col);

        s = s.replaceAll("_", "");
        int sign = 1;

        if (s.startsWith("+")) {
            s = s.substring(1);
        } else if (s.startsWith("-")) {
            s = s.substring(1);
            sign = 