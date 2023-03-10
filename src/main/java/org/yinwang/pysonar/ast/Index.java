
package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Index extends Node {

    public Node value;

    public Index(Node n, String file, int start, int end, int line, int col) {
        super(NodeType.INDEX, file, start, end, line, col);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Index:" + value + ">";
    }

}