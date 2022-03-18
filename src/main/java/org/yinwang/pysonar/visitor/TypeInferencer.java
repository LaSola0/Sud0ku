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
        node.addSpecialAttribute(classType.table, "__name__", Types.StrInstance);
        node.addSpecialAttribute(classType.table, "__dict__",
                                 new DictType(Types.StrInstance, Types.UNKNOWN));
        node.addSpecialAttribute(classType.table, "__module__", Types.StrInstance);
        node.addSpecialAttribute(classType.table, "__doc__", Types.StrInstance);

        // Bind ClassType to name here before resolving the body because the
        // methods need node type as self.
        bind(s, node.name, classType, CLASS);
        if (node.body != null)
        {
            visit(node.body, classType.table);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Comprehension node, State s)
    {
        bindIter(s, node.target, node.iter, SCOPE);
        visit(node.ifs, s);
        return visit(node.target, s);
    }

    @NotNull
    @Override
    public Type visit(Continue node, State s)
    {
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Delete node, State s)
    {
        for (Node n : node.targets)
        {
            visit(n, s);
            if (n instanceof Name)
            {
                s.remove(((Name) n).id);
            }
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Dict node, State s)
    {
        Type keyType = resolveUnion(node.keys, s);
        Type valType = resolveUnion(node.values, s);
        return new DictType(keyType, valType);
    }

    @NotNull
    @Override
    public Type visit(DictComp node, State s)
    {
        visit(node.generators, s);
        Type keyType = visit(node.key, s);
        Type valueType = visit(node.value, s);
        return new DictType(keyType, valueType);
    }

    @NotNull
    @Override
    public Type visit(Dummy node, State s)
    {
        return Types.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Ellipsis node, State s)
    {
        return Types.NoneInstance;
    }

    @NotNull
    @Override
    public Type visit(Exec node, State s)
    {
        if (node.body != null)
        {
            visit(node.body, s);
        }
        if (node.globals != null)
        {
            visit(node.globals, s);
        }
        if (node.locals != null)
        {
            visit(node.locals, s);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Expr node, State s)
    {
        if (node.value != null)
        {
            visit(node.value, s);
        }
        return Types.CONT;

    }

    @NotNull
    @Override
    public Type visit(ExtSlice node, State s)
    {
        for (Node d : node.dims)
        {
            visit(d, s);
        }
        return new ListType();
    }

    @NotNull
    @Override
    public Type visit(For node, State s)
    {
        bindIter(s, node.target, node.iter, SCOPE);
        Type t1 = Types.UNKNOWN;
        Type t2 = Types.UNKNOWN;
        Type t3 = Types.UNKNOWN;

        State s1 = s.copy();
        State s2 = s.copy();

        if (node.body != null)
        {
            t1 = visit(node.body, s1);
            s.merge(s1);
            t2 = visit(node.body, s1);
            s.merge(s1);
        }

        if (node.orelse != null)
        {
            t3 = visit(node.orelse, s2);
            s.merge(s2);
        }

        return UnionType.union(t1, t2, t3);
    }

    @NotNull
    @Override
    public Type visit(FunctionDef node, State s)
    {
        State env = s.getForwarding();
        FunType fun = new FunType(node, env);
        fun.table.setParent(s);
        fun.table.setPath(s.extendPath(node.name.id));
        fun.setDefaultTypes(visit(node.defaults, s));
        Analyzer.self.addUncalled(fun);
        Binding.Kind funkind;

        if (node.isLamba)
        {
            return fun;
        }
        else
        {
            if (s.stateType == State.StateType.CLASS)
            {
                if ("__init__".equals(node.name.id))
                {
                    funkind = CONSTRUCTOR;
                }
                else
                {
                    funkind = METHOD;
                }
            }
            else
            {
                funkind = FUNCTION;
            }

            Type outType = s.type;
            if (outType instanceof ClassType)
            {
                fun.setCls((ClassType) outType);
            }

            bind(s, node.name, fun, funkind);
            return Types.CONT;
        }
    }

    @NotNull
    @Override
    public Type visit(GeneratorExp node, State s)
    {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Global node, State s)
    {
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Handler node, State s)
    {
        Type typeval = Types.UNKNOWN;
        if (node.exceptions != null)
        {
            typeval = resolveUnion(node.exceptions, s);
        }
        if (node.binder != null)
        {
            bind(s, node.binder, typeval);
        }
        if (node.body != null)
        {
            return visit(node.body, s);
        }
        else
        {
            return Types.UNKNOWN;
        }
    }

    @NotNull
    @Override
    public Type visit(If node, State s)
    {
        Type type1, type2;
        State s1 = s.copy();
        State s2 = s.copy();

        // Ignore result because Python can treat anything as bool
        visit(node.test, s);
        inferInstance(node.test, s, s1);

        if (node.body != null)
        {
            type1 = visit(node.body, s1);
        }
        else
        {
            type1 = Types.CONT;
        }

        if (node.orelse != null)
        {
            type2 = visit(node.orelse, s2);
        }
        else
        {
            type2 = Types.CONT;
        }

        boolean cont1 = UnionType.contains(type1, Types.CONT);
        boolean cont2 = UnionType.contains(type2, Types.CONT);

        // decide which branch affects the downstream state
        if (cont1 && cont2)
        {
            s1.merge(s2);
            s.overwrite(s1);
        }
        else if (cont1)
        {
            s.overwrite(s1);
        }
        else if (cont2)
        {
            s.overwrite(s2);
        }

        return UnionType.union(type1, type2);
    }

    /**
     * Helper for branch inference for 'isinstance'
     */
    private void inferInstance(Node test, State s, State s1)
    {
        if (test instanceof Call)
        {
            Call testCall = (Call) test;
            if (testCall.func instanceof Name)
            {
                Name testFunc = (Name) testCall.func;
                if (testFunc.id.equals("isinstance"))
                {
                    if (testCall.args.size() >= 2)
                    {
                        Node id = testCall.args.get(0);
                        if (id instanceof Name)
                        {
                            Node typeExp = testCall.args.get(1);
                            Type type = visit(typeExp, s);
                            if (type instanceof ClassType)
                            {
                                type = ((ClassType) type).getInstance(null, this, test);
                            }
                            s1.insert(((Name) id).id, id, type, VARIABLE);
                        }
                    }

                    if (testCall.args.size() != 2)
                    {
                        addWarningToNode(test, "Incorrect number of arguments for isinstance");
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    public Type visit(IfExp node, State s)
    {
        Type type1, type2;
        visit(node.test, s);

        if (node.body != null)
        {
            type1 = visit(node.body, s);
        }
        else
        {
            type1 = Types.CONT;
        }
        if (node.orelse != null)
        {
            type2 = visit(node.orelse, s);
        }
        else
        {
            type2 = Types.CONT;
        }
        return UnionType.union(type1, type2);
    }

    @NotNull
    @Override
    public Type visit(Import node, State s)
    {
        for (Alias a : node.names)
        {
            Type mod = Analyzer.self.loadModule(a.name, s);
            if (mod == null)
            {
                addWarningToNode(node, "Cannot load module");
            }
            else if (a.asname != null)
            {
                s.insert(a.asname.id, a.asname, mod, VARIABLE);
            }
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(ImportFrom node, State s)
    {
        if (node.module == null)
        {
            return Types.CONT;
        }

        Type mod = Analyzer.self.loadModule(node.module, s);

        if (mod == null)
        {
            addWarningToNode(node, "Cannot load module");
        }
        else if (node.isImportStar())
        {
            node.importStar(s, mod);
        }
        else
        {
            for (Alias a : node.names)
            {
                Name first = a.name.get(0);
                Set<Binding> bs = mod.table.lookup(first.id);
                if (bs != null)
                {
                    if (a.asname != null)
                    {
                        s.update(a.asname.id, bs);
                        Analyzer.self.putRef(a.asname, bs);
                    }
                    else
                    {
                        s.update(first.id, bs);
                        Analyzer.self.putRef(first, bs);
                    }
                }
                else
                {
                    List<Name> ext = new ArrayList<>(node.module);
                    ext.add(first);
                    Type mod2 = Analyzer.self.loadModule(ext, s);
                    if (mod2 != null)
                    {
                        if (a.asname != null)
                        {
                            Binding binding = Binding.createFileBinding(a.asname.id, mod2.file, mod2);
                            s.update(a.asname.id, binding);
                            Analyzer.self.putRef(a.asname, binding);
                        }
                        else
                        {
                            Binding binding = Binding.createFileBinding(first.id, mod2.file, mod2);
                            s.update(first.id, binding);
                            Analyzer.self.putRef(first, binding);
                        }
                    }
                }
            }
        }

        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Index node, State s)
    {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(Keyword node, State s)
    {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(ListComp node, State s)
    {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Name node, State s)
    {
        Set<Binding> b = s.lookup(node.