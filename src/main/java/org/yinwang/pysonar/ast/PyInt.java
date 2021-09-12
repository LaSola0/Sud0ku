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
            sign = -1;
        }

        int base;
        if (s.startsWith("0b")) {
            base = 2;
            s = s.substring(2);
        } else if (s.startsWith("0x")) {
       