package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DictComp extends Node {

    public Node key;
    public Node value;
    public List<Comprehension> generators;

    public DictComp(Node key, Node value, List<Comprehension> generators, String file, int start, int end, int line