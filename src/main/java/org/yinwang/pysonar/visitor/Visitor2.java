
package org.yinwang.pysonar.visitor;


import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.ast.*;

import java.util.ArrayList;
import java.util.List;

public interface Visitor2<T, P, Q> {

    default T visit(@NotNull Node node, P param1, Q param2) {
        switch (node.nodeType) {
            case ALIAS:
                return visit((Alias)node, param1, param2);
            case ASSERT:
                return visit((Assert)node, param1, param2);
            case ASSIGN:
                return visit((Assign)node, param1, param2);
            case ATTRIBUTE:
                return visit((Attribute)node, param1, param2);
            case AWAIT:
                return visit((Await)node, param1, param2);
            case BINOP:
                return visit((BinOp)node, param1, param2);
            case BLOCK:
                return visit((Block)node, param1, param2);
            case BREAK:
                return visit((Break)node, param1, param2);
            case BYTES:
                return visit((Bytes)node, param1, param2);
            case CALL:
                return visit((Call)node, param1, param2);
            case CLASSDEF:
                return visit((ClassDef)node, param1, param2);
            case COMPREHENSION:
                return visit((Comprehension)node, param1, param2);
            case CONTINUE:
                return visit((Continue)node, param1, param2);
            case DELETE:
                return visit((Delete)node, param1, param2);
            case DICT:
                return visit((Dict)node, param1, param2);
            case DICTCOMP:
                return visit((DictComp)node, param1, param2);
            case DUMMY:
                return visit((Dummy)node, param1, param2);
            case ELLIPSIS:
                return visit((Ellipsis)node, param1, param2);
            case EXEC:
                return visit((Exec)node, param1, param2);
            case EXPR:
                return visit((Expr)node, param1, param2);
            case EXTSLICE:
                return visit((ExtSlice)node, param1, param2);
            case FOR:
                return visit((For)node, param1, param2);
            case FUNCTIONDEF:
                return visit((FunctionDef)node, param1, param2);
            case GENERATOREXP:
                return visit((GeneratorExp)node, param1, param2);
            case GLOBAL:
                return visit((Global)node, param1, param2);
            case HANDLER:
                return visit((Handler)node, param1, param2);
            case IF:
                return visit((If)node, param1, param2);
            case IFEXP:
                return visit((IfExp)node, param1, param2);
            case IMPORT:
                return visit((Import)node, param1, param2);
            case IMPORTFROM:
                return visit((ImportFrom)node, param1, param2);
            case INDEX:
                return visit((Index)node, param1, param2);
            case KEYWORD:
                return visit((Keyword)node, param1, param2);
            case LISTCOMP:
                return visit((ListComp)node, param1, param2);
            case MODULE:
                return visit((PyModule)node, param1, param2);
            case NAME:
                return visit((Name)node, param1, param2);
            case PASS:
                return visit((Pass)node, param1, param2);
            case PRINT:
                return visit((Print)node, param1, param2);
            case PYCOMPLEX:
                return visit((PyComplex)node, param1, param2);
            case PYFLOAT:
                return visit((PyFloat)node, param1, param2);
            case PYINT:
                return visit((PyInt)node, param1, param2);
            case PYLIST:
                return visit((PyList)node, param1, param2);
            case PYSET:
                return visit((PySet)node, param1, param2);