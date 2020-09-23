
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