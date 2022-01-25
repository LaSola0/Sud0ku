package org.yinwang.pysonar.visitor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.$;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Builtins;
import org.yinwang.pysonar.CallStackEntry;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.types.ClassType;
import org.yinwang.pysonar.types.DictType;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.InstanceType;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.TupleType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.Types;
import org.yinwang.pysonar.types.UnionType;

import static org.yinwang.pysonar.Binding.Kind.ATTRIBUTE;
import static org.yinwang.pysonar.Binding.Kind.CLASS;
import static org.yinwang.pysonar.Binding.Kind.CONSTRUCTOR;
import static org.yinwang.pysonar.Binding.Kind.FUNCTION;
import static org.yinwang.pysonar.Binding.Kind.METHOD;
import static org.yinwang.pysonar.Binding.Kind.MODULE;
import static org.yinwang.pysonar.Binding.Kind.PARAMETER;
import static org.yinwang.pysonar.Binding.Kind.SCOPE;
import static org.yinwang.pysonar.Binding.Kind.VARIABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeInferencer implements Visitor1<Type, State>
{

    @NotNull
    @Override
    public Type visit(PyModule node, State s)
    {
        ModuleType mt = new ModuleType(node.name, node.file, Analyzer.self.globaltable);
        s.insert($.moduleQname(node.file), node, mt, MODULE);
        if (node.body != null)
        {
            visit(node.body, mt.table);
        }
        return m