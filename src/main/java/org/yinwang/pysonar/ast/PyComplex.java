package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class PyComplex extends Node {

    public double real;
    public double imag;

    public PyComplex(double real, double imag, Stri