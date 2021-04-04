package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Comprehension extends Node {

    public Node target;
    public Node iter;
    public List<Node> ifs;

    public Comprehension(Node target, Node iter, List<Node> ifs, String file,