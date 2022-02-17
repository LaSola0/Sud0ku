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
        return mt;
    }

    @NotNull
    @Override
    public Type visit(Alias node, State s)
    {
        return Types.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Assert node, State s)
    {
        if (node.test != null)
        {
            visit(node.test, s);
        }
        if (node.msg != null)
        {
            visit(node.msg, s);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Assign node, State s)
    {
        Type valueType = visit(node.value, s);
        bind(s, node.target, valueType);
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Attribute node, State s)
    {
        Type targetType = visit(node.target, s);
        if (targetType instanceof UnionType)
        {
            Set<Type> types = ((UnionType) targetType).types;
            Type retType = Types.UNKNOWN;
            for (Type tt : types)
            {
                retType = UnionType.union(retType, getAttrType(node, tt));
            }
            return retType;
        }
        else
        {
            return getAttrType(node, targetType);
        }
    }

    @NotNull
    @Override
    public Type visit(Await node, State s)
    {
        if (node.value == null)
        {
            return Types.NoneInstance;
        }
        else
        {
            return visit(node.value, s);
        }
    }

    @NotNull
    @Override
    public Type visit(BinOp node, State s)
    {
        Type ltype = visit(node.left, s);
        Type rtype = visit(node.right, s);
        if (operatorOverridden(ltype, node.op.getMethod()))
        {
            Type result = applyOp(node.op, ltype, rtype, node.op.getMethod(), node, node.left);
            if (result != null)
            {
                return result;
            }
        }
        else if (Op.isBoolean(node.op))
        {
            return Types.BoolInstance;
        }
        else if (ltype == Types.UNKNOWN)
        {
            return rtype;
        }
        else if (rtype == Types.UNKNOWN)
        {
            return ltype;
        }
        else if (ltype.typeEquals(rtype))
        {
            return ltype;
        }
        else if (node.op == Op.Or)
        {
            if (rtype == Types.NoneInstance)
            {
                return ltype;
            }
            else if (ltype == Types.NoneInstance)
            {
                return rtype;
            }
        }
        else if (node.op == Op.And)
        {
            if (rtype == Types.NoneInstance || ltype == Types.NoneInstance)
            {
                return Types.NoneInstance;
            }
        }

        addWarningToNode(node,
                         "Cannot apply binary operator " + node.op.getRep() + " to type " + ltype + " and " + rtype);
        return Types.UNKNOWN;
    }

    private boolean operatorOverridden(Type type, String method)
    {
        if (type instanceof InstanceType)
        {
            Type opType = type.table.lookupAttrType(method);
            if (opType != null)
            {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Type applyOp(Op op, Type ltype, Type rtype, String method, Node node, Node left)
    {
        Type opType = ltype.table.lookupAttrType(method);
        if (opType instanceof FunType)
        {
            return apply((FunType) opType, ltype, Collections.singletonList(rtype), null, null, null, node);
        }
        else
        {
            addWarningToNode(left, "Operator method " + method + " is not a function");
            return null;
        }
    }

    @NotNull
    @Override
    public Type visit(Block node, State s)
    {
        // first pass: mark global names
        for (Node n : node.seq)
        {
            if (n instanceof Global)
            {
                for (Name name : ((Global) n).names)
                {
                    s.addGlobalName(name.id);
                    Set<Binding> nb = s.lookup(name.id);
                    if (nb != null)
                    {
                        Analyzer.self.putRef(name, nb);
                    }
                }
            }
        }

        boolean returned = false;
        Type retType = Types.UNKNOWN;

        for (Node n : node.seq)
        {
            Type t = visit(n, s);
            if (!returned)
            {
                retType = UnionType.union(retType, t);
                if (!UnionType.contains(t, Types.CONT))
                {
                    returned = true;
                    retType = UnionType.remove(retType, Types.CONT);
                }
            }
        }

        return retType;
    }

    @NotNull
    @Override
    public Type visit(Break node, State s)
    {
        return Types.NoneInstance;
    }

    @NotNull
    @Override
    public Type visit(Bytes node, State s)
    {
        return Types.StrInstance;
    }

    @NotNull
    @Override
    public Type visit(Call node, State s)
    {
        Type fun;
        Type selfType = null;

        if (node.func instanceof Attribute)
        {
            Node target = ((Attribute) node.func).target;
            Name attr = ((Attribute) node.func).attr;
            Type targetType = visit(target, s);
            if (!(targetType instanceof ModuleType))
            {
                selfType = targetType;
            }
            Set<Binding> b = targetType.table.lookupAttr(attr.id);
            if (b != null)
            {
                Analyzer.self.putRef(attr, b);
                fun = State.makeUnion(b);
            }
            else
            {
                Analyzer.self.putProblem(attr, "Attribute is not found in type: " + attr.id);
                fun = Types.UNKNOWN;
            }
        }
        else
        {
            fun = visit(node.func, s);
        }

        // Infer positional argument types
        List<Type> positional = visit(node.args, s);

        // Infer keyword argument types
        Map<String, Type> kwTypes = new HashMap<>();
        if (node.keywords != null)
        {
            for (Keyword k : node.keywords)
            {
                kwTypes.put(k.arg, visit(k.value, s));
            }
        }

        Type kwArg = node.kwargs == null ? null : visit(node.kwargs, s);
        Type starArg = node.starargs == null ? null : visit(node.starargs, s);

        if (fun instanceof UnionType)
        {
            Set<Type> types = ((UnionType) fun).types;
            Type resultType = Types.UNKNOWN;
            for (Type funType : types)
            {
                Type returnType = resolveCall(funType, selfType, positional, kwTypes, kwArg, starArg, node);
                resultType = UnionType.union(resultType, returnType);
            }
            return resultType;
        }
        else
        {
            return resolveCall(fun, selfType, positional, kwTypes, kwArg, starArg, node);
        }
    }

    @NotNull
    @Override
    public Type visit(ClassDef node, State s)
    {
        ClassType classType = new ClassType(node.name.id, s);
        List<Type> baseTypes = new ArrayList<>();
        for (Node base : node.bases)
        {
            Type baseType = visit(base, s);
            if (baseType instanceof ClassType)
            {
                classType.addSuper(baseType);
            }
            else if (baseType instanceof UnionType)
            {
                for (Type parent : ((UnionType) baseType).types)
                {
                    classType.addSuper(parent);
                }
            }
            else
            {
                addWarningToNode(base, base + " is not a class");
            }
            baseTypes.add(baseType);
        }

        // XXX: Not sure if we should add "bases", "name" and "dict" here. They
        // must be added _somewhere_ but I'm just not sure if it should be HERE.
        node.addSpecialAttribute(classType.table, "__bases__", new TupleType(baseTypes));
        node.