
package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.PyModule;
import org.yinwang.pysonar.ast.Url;
import org.yinwang.pysonar.types.*;

import java.util.HashMap;
import java.util.Map;

import static org.yinwang.pysonar.Binding.Kind.*;

/**
 * This file is messy. Should clean up.
 */
public class Builtins {

    public static final String LIBRARY_URL = "http://docs.python.org/library/";
    public static final String TUTORIAL_URL = "http://docs.python.org/tutorial/";
    public static final String REFERENCE_URL = "http://docs.python.org/reference/";
    public static final String DATAMODEL_URL = "http://docs.python.org/reference/datamodel#";


    @NotNull
    public static Url newLibUrl(String module, String name) {
        return newLibUrl(module + ".html#" + name);
    }


    @NotNull
    public static Url newLibUrl(@NotNull String path) {
        if (!path.contains("#") && !path.endsWith(".html")) {
            path += ".html";
        }
        return new Url(LIBRARY_URL + path);
    }


    @NotNull
    public static Url newRefUrl(String path) {
        return new Url(REFERENCE_URL + path);
    }


    @NotNull
    public static Url newDataModelUrl(String path) {
        return new Url(DATAMODEL_URL + path);
    }


    @NotNull
    public static Url newTutUrl(String path) {
        return new Url(TUTORIAL_URL + path);
    }


    // XXX:  need to model "types" module and reconcile with these types
    public ModuleType Builtin;
    public ClassType objectType;
    public ClassType BaseType;
    public ClassType BaseList;
    public InstanceType BaseListInst;
    public ClassType BaseArray;
    public ClassType BaseTuple;
    public ClassType BaseModule;
    public ClassType BaseFile;
    public InstanceType BaseFileInst;
    public ClassType BaseException;
    public ClassType BaseStruct;
    public ClassType BaseFunction;  // models functions, lambas and methods
    public ClassType BaseClass;  // models classes and instances

    public ClassType Datetime_datetime;
    public ClassType Datetime_date;
    public ClassType Datetime_time;
    public ClassType Datetime_timedelta;
    public ClassType Datetime_tzinfo;
    public InstanceType Time_struct_time;


    @NotNull
    String[] builtin_exception_types = {
            "ArithmeticError", "AssertionError", "AttributeError",
            "BaseException", "Exception", "DeprecationWarning", "EOFError",
            "EnvironmentError", "FloatingPointError", "FutureWarning",
            "GeneratorExit", "IOError", "ImportError", "ImportWarning",
            "IndentationError", "IndexError", "KeyError", "KeyboardInterrupt",
            "LookupError", "MemoryError", "NameError", "NotImplemented",
            "NotImplementedError", "OSError", "OverflowError",
            "PendingDeprecationWarning", "ReferenceError", "RuntimeError",
            "RuntimeWarning", "StandardError", "StopIteration", "SyntaxError",
            "SyntaxWarning", "SystemError", "SystemExit", "TabError",
            "TypeError", "UnboundLocalError", "UnicodeDecodeError",
            "UnicodeEncodeError", "UnicodeError", "UnicodeTranslateError",
            "UnicodeWarning", "UserWarning", "ValueError", "Warning",
            "ZeroDivisionError"
    };


    @NotNull
    ClassType newClass(@NotNull String name, State table) {
        return newClass(name, table, null);
    }


    @NotNull
    ClassType newClass(@NotNull String name, State table,
                       ClassType superClass, @NotNull ClassType... moreSupers)
    {
        ClassType t = new ClassType(name, table, superClass);
        for (ClassType c : moreSupers) {
            t.addSuper(c);
        }
        return t;
    }


    @Nullable
    ModuleType newModule(String name) {
        return new ModuleType(name, null, Analyzer.self.globaltable);
    }


    @NotNull
    ClassType newException(@NotNull String name, State t) {
        return newClass(name, t, BaseException);
    }


    @NotNull
    FunType newFunc() {
        return new FunType();
    }


    @Nullable
    FunType newFunc(@Nullable Type type) {
        if (type == null) {
            type = Types.UNKNOWN;
        }
        return new FunType(Types.UNKNOWN, type);
    }


    @NotNull
    ListType newList() {
        return newList(Types.UNKNOWN);
    }


    @NotNull
    ListType newList(Type type) {
        return new ListType(type);
    }


    @NotNull
    DictType newDict(Type ktype, Type vtype) {
        return new DictType(ktype, vtype);
    }


    @NotNull
    TupleType newTuple(Type... types) {
        return new TupleType(types);
    }


    @NotNull
    UnionType newUnion(Type... types) {
        return new UnionType(types);
    }


    String[] list(String... names) {
        return names;
    }


    private abstract class NativeModule {

        protected String name;
        @Nullable
        protected ModuleType module;
        @Nullable
        protected State table;  // the module's symbol table


        NativeModule(String name) {
            this.name = name;
            modules.put(name, this);
        }


        /**
         * Lazily load the module.
         */
        @Nullable
        ModuleType getModule() {
            if (module == null) {
                createModuleType();
                initBindings();
            }
            return module;
        }


        protected abstract void initBindings();


        protected void createModuleType() {
            if (module == null) {
                module = newModule(name);
                table = module.table;
                Analyzer.self.moduleTable.insert(name, liburl(), module, MODULE);
            }
        }


        @Nullable
        protected void update(String name, Url url, Type type, Binding.Kind kind) {
            table.insert(name, url, type, kind);
        }


        @Nullable
        protected void addClass(String name, Url url, Type type) {
            table.insert(name, url, type, CLASS);
        }


        @Nullable
        protected void addClass(ClassType type) {
            table.insert(type.name, liburl(type.name), type, CLASS);
        }


        @Nullable
        protected void addMethod(ClassType cls, String name, Type type) {
            cls.table.insert(name, liburl(cls.name + "." + name), newFunc(type), METHOD);
        }

        @Nullable
        protected void addMethod(ClassType cls, String name) {
            cls.table.insert(name, liburl(cls.name + "." + name), newFunc(), METHOD);
        }


        protected void addFunction(ModuleType module, String name, Type type) {
            Url url = this.module == module ? liburl(module.qname + "." + name) :
                newLibUrl(module.table.path, module.table.path + "." + name);
            module.table.insert(name, url, newFunc(type), FUNCTION);
        }


        protected void addFunction(String name, Type type) {
            addFunction(module, name, type);
        }


        // don't use this unless you're sure it's OK to share the type object
        protected void addFunctions_beCareful(Type type, @NotNull String... names) {
            for (String name : names) {
                addFunction(name, type);
            }
        }


        protected void addNoneFuncs(String... names) {
            addFunctions_beCareful(Types.NoneInstance, names);
        }


        protected void addNumFuncs(String... names) {
            addFunctions_beCareful(Types.IntInstance, names);
        }


        protected void addStrFuncs(String... names) {
            addFunctions_beCareful(Types.StrInstance, names);
        }

