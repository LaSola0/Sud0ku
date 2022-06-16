
package org.yinwang.pysonar.visitor;


import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;

public interface Visitor1<T, P> {

    default T visit(@NotNull Node node, P param) {
        switch (node.nodeType) {
            case ALIAS:
                return visit((Alias)node, param);
            case ASSERT:
                return visit((Assert)node, param);
            case ASSIGN:
                return visit((Assign)node, param);
            case ATTRIBUTE:
                return visit((Attribute)node, param);
            case AWAIT:
                return visit((Await)node, param);
            case BINOP:
                return visit((BinOp)node, param);
            case BLOCK:
                return visit((Block)node, param);
            case BREAK:
                return visit((Break)node, param);
            case BYTES:
                return visit((Bytes)node, param);
            case CALL:
                return visit((Call)node, param);
            case CLASSDEF:
                return visit((ClassDef)node, param);
            case COMPREHENSION:
                return visit((Comprehension)node, param);
            case CONTINUE:
                return visit((Continue)node, param);
            case DELETE:
                return visit((Delete)node, param);
            case DICT:
                return visit((Dict)node, param);
            case DICTCOMP:
                return visit((DictComp)node, param);
            case DUMMY:
                return visit((Dummy)node, param);
            case ELLIPSIS:
                return visit((Ellipsis)node, param);
            case EXEC:
                return visit((Exec)node, param);
            case EXPR:
                return visit((Expr)node, param);
            case EXTSLICE:
                return visit((ExtSlice)node, param);
            case FOR:
                return visit((For)node, param);
            case FUNCTIONDEF:
                return visit((FunctionDef)node, param);
            case GENERATOREXP:
                return visit((GeneratorExp)node, param);
            case GLOBAL:
                return visit((Global)node, param);
            case HANDLER:
                return visit((Handler)node, param);
            case IF:
                return visit((If)node, param);
            case IFEXP:
                return visit((IfExp)node, param);
            case IMPORT:
                return visit((Import)node, param);
            case IMPORTFROM:
                return visit((ImportFrom)node, param);
            case INDEX:
                return visit((Index)node, param);
            case KEYWORD:
                return visit((Keyword)node, param);
            case LISTCOMP:
                return visit((ListComp)node, param);
            case MODULE:
                return visit((PyModule)node, param);
            case NAME:
                return visit((Name)node, param);
            case NODE:
                return visit((Node)node, param);
            case PASS:
                return visit((Pass)node, param);
            case PRINT:
                return visit((Print)node, param);
            case PYCOMPLEX:
                return visit((PyComplex)node, param);
            case PYFLOAT:
                return visit((PyFloat)node, param);
            case PYINT:
                return visit((PyInt)node, param);
            case PYLIST:
                return visit((PyList)node, param);
            case PYSET:
                return visit((PySet)node, param);
            case RAISE:
                return visit((Raise)node, param);
            case REPR:
                return visit((Repr)node, param);
            case RETURN:
                return visit((Return)node, param);
            case SEQUENCE:
                return visit((Sequence)node, param);
            case SETCOMP:
                return visit((SetComp)node, param);
            case SLICE:
                return visit((Slice)node, param);
            case STARRED:
                return visit((Starred)node, param);
            case STR:
                return visit((Str)node, param);
            case SUBSCRIPT:
                return visit((Subscript)node, param);
            case TRY:
                return visit((Try)node, param);
            case TUPLE:
                return visit((Tuple)node, param);
            case UNARYOP:
                return visit((UnaryOp)node, param);
            case UNSUPPORTED:
                return visit((Unsupported)node, param);
            case URL:
                return visit((Url)node, param);
            case WHILE:
                return visit((While)node, param);
            case WITH:
                return visit((With)node, param);
            case WITHITEM:
                return visit((Withitem)node, param);
            case YIELD:
                return visit((Yield)node, param);
            case YIELDFROM:
                return visit((YieldFrom)node, param);

            default: