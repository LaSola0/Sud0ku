
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


        protected void addUnknownFuncs(@NotNull String... names) {
            for (String name : names) {
                addFunction(name, Types.UNKNOWN);
            }
        }


        protected void addAttr(String name, Url url, Type type) {
            table.insert(name, url, type, ATTRIBUTE);
        }


        protected void addAttr(String name, Type type) {
            addAttr(table, name, type);
        }


        protected void addAttr(State s, String name, Type type) {
            s.insert(name, liburl(s.path + "." + name), type, ATTRIBUTE);
        }


        protected void addAttr(ClassType cls, String name, Type type) {
            addAttr(cls.table, name, type);
        }

        // don't use this unless you're sure it's OK to share the type object
        protected void addAttributes_beCareful(Type type, @NotNull String... names) {
            for (String name : names) {
                addAttr(name, type);
            }
        }


        protected void addNumAttrs(String... names) {
            addAttributes_beCareful(Types.IntInstance, names);
        }


        protected void addStrAttrs(String... names) {
            addAttributes_beCareful(Types.StrInstance, names);
        }


        protected void addUnknownAttrs(@NotNull String... names) {
            for (String name : names) {
                addAttr(name, Types.UNKNOWN);
            }
        }


        @NotNull
        protected Url liburl() {
            return newLibUrl(name);
        }


        @NotNull
        protected Url liburl(String anchor) {
            return newLibUrl(name, anchor);
        }

        @NotNull
        @Override
        public String toString() {
            return module == null
                    ? "<Non-loaded builtin module '" + name + "'>"
                    : "<NativeModule:" + module + ">";
        }
    }


    /**
     * The set of top-level native modules.
     */
    @NotNull
    private Map<String, NativeModule> modules = new HashMap<>();


    public Builtins() {
        buildTypes();
    }


    private void buildTypes() {
        new BuiltinsModule();
        State bt = Builtin.table;

        objectType = newClass("object", bt);
        BaseType = newClass("type", bt, objectType);
        BaseTuple = newClass("tuple", bt, objectType);
        BaseList = newClass("list", bt, objectType);
        BaseListInst = new InstanceType(BaseList);
        BaseArray = newClass("array", bt);
        ClassType numClass = newClass("int", bt, objectType);
        BaseModule = newClass("module", bt);
        BaseFile = newClass("file", bt, objectType);
        BaseFileInst = new InstanceType(BaseFile);
        BaseFunction = newClass("function", bt, objectType);
        BaseClass = newClass("classobj", bt, objectType);
    }


    void init() {
        buildObjectType();
        buildTupleType();
        buildArrayType();
        buildListType();
        buildDictType();
        buildNumTypes();
        buildStrType();
        buildModuleType();
        buildFileType();
        buildFunctionType();
        buildClassType();

        modules.get("__builtin__").initBindings();  // eagerly load these bindings

        new ArrayModule();
        new AudioopModule();
        new BinasciiModule();
        new Bz2Module();
        new CPickleModule();
        new CStringIOModule();
        new CMathModule();
        new CollectionsModule();
        new CryptModule();
        new CTypesModule();
        new DatetimeModule();
        new DbmModule();
        new ErrnoModule();
        new ExceptionsModule();
        new FcntlModule();
        new FpectlModule();
        new GcModule();
        new GdbmModule();
        new GrpModule();
        new ImpModule();
        new ItertoolsModule();
        new MarshalModule();
        new MathModule();
        new Md5Module();
        new MmapModule();
        new NisModule();
        new OperatorModule();
        new OsModule();
        new ParserModule();
        new PosixModule();
        new PwdModule();
        new PyexpatModule();
        new ReadlineModule();
        new ResourceModule();
        new SelectModule();
        new SignalModule();
        new ShaModule();
        new SpwdModule();
        new StropModule();
        new StructModule();
        new SysModule();
        new SyslogModule();
        new TermiosModule();
        new ThreadModule();
        new TimeModule();
        new UnicodedataModule();
        new ZipimportModule();
        new ZlibModule();
        new UnittestModule();
    }


    /**
     * Loads (if necessary) and returns the specified built-in module.
     */
    @Nullable
    public ModuleType get(@NotNull String name) {
        if (!name.contains(".")) {  // unqualified
            return getModule(name);
        }

        String[] mods = name.split("\\.");
        Type type = getModule(mods[0]);
        if (type == null) {
            return null;
        }
        for (int i = 1; i < mods.length; i++) {
            type = type.table.lookupType(mods[i]);
            if (!(type instanceof ModuleType)) {
                return null;
            }
        }
        return (ModuleType) type;
    }


    @Nullable
    private ModuleType getModule(String name) {
        NativeModule wrap = modules.get(name);
        return wrap == null ? null : wrap.getModule();
    }


    void buildObjectType() {
        String[] obj_methods = {
                "__delattr__", "__format__", "__getattribute__", "__hash__",
                "__init__", "__new__", "__reduce__", "__reduce_ex__",
                "__repr__", "__setattr__", "__sizeof__", "__str__", "__subclasshook__"
        };
        for (String m : obj_methods) {
            objectType.table.insert(m, newLibUrl("stdtypes"), newFunc(), METHOD);
        }
        objectType.table.insert("__doc__", newLibUrl("stdtypes"), Types.StrInstance, CLASS);
        objectType.table.insert("__class__", newLibUrl("stdtypes"), Types.UNKNOWN, CLASS);
    }


    void buildTupleType() {
        State bt = BaseTuple.table;
        String[] tuple_methods = {
                "__add__", "__contains__", "__eq__", "__ge__", "__getnewargs__",
                "__gt__", "__iter__", "__le__", "__len__", "__lt__", "__mul__",
                "__ne__", "__new__", "__rmul__", "count", "index"
        };
        for (String m : tuple_methods) {
            bt.insert(m, newLibUrl("stdtypes"), newFunc(), METHOD);
        }
        bt.insert("__getslice__", newDataModelUrl("object.__getslice__"), newFunc(), METHOD);
        bt.insert("__getitem__", newDataModelUrl("object.__getitem__"), newFunc(), METHOD);
        bt.insert("__iter__", newDataModelUrl("object.__iter__"), newFunc(), METHOD);
    }


    void buildArrayType() {
        String[] array_methods_none = {
                "append", "buffer_info", "byteswap", "extend", "fromfile",
                "fromlist", "fromstring", "fromunicode", "index", "insert", "pop",
                "read", "remove", "reverse", "tofile", "tolist", "typecode", "write"
        };
        for (String m : array_methods_none) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.NoneInstance), METHOD);
        }
        String[] array_methods_num = {"count", "itemsize",};
        for (String m : array_methods_num) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.IntInstance), METHOD);
        }
        String[] array_methods_str = {"tostring", "tounicode",};
        for (String m : array_methods_str) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.StrInstance), METHOD);
        }
    }


    void buildListType() {
        BaseList.table.insert("__getslice__", newDataModelUrl("object.__getslice__"),
                newFunc(BaseListInst), METHOD);
        BaseList.table.insert("__getitem__", newDataModelUrl("object.__getitem__"),
                newFunc(BaseList), METHOD);
        BaseList.table.insert("__iter__", newDataModelUrl("object.__iter__"),
                newFunc(BaseList), METHOD);

        String[] list_methods_none = {
                "append", "extend", "index", "insert", "pop", "remove", "reverse", "sort"
        };
        for (String m : list_methods_none) {
            BaseList.table.insert(m, newLibUrl("stdtypes"), newFunc(Types.NoneInstance), METHOD);
        }
        String[] list_methods_num = {"count"};
        for (String m : list_methods_num) {
            BaseList.table.insert(m, newLibUrl("stdtypes"), newFunc(Types.IntInstance), METHOD);
        }
    }


    @NotNull
    Url numUrl() {
        return newLibUrl("stdtypes", "typesnumeric");
    }


    void buildNumTypes() {
        State bft = Types.FloatInstance.table;
        String[] float_methods_num = {
                "__abs__", "__add__", "__coerce__", "__div__", "__divmod__",
                "__eq__", "__float__", "__floordiv__", "__format__",
                "__ge__", "__getformat__", "__gt__", "__int__",
                "__le__", "__long__", "__lt__", "__mod__", "__mul__", "__ne__",
                "__neg__", "__new__", "__nonzero__", "__pos__", "__pow__",
                "__radd__", "__rdiv__", "__rdivmod__", "__rfloordiv__", "__rmod__",
                "__rmul__", "__rpow__", "__rsub__", "__rtruediv__", "__setformat__",
                "__sub__", "__truediv__", "__trunc__", "as_integer_ratio",
                "fromhex", "is_integer"
        };
        for (String m : float_methods_num) {
            bft.insert(m, numUrl(), newFunc(Types.FloatInstance), METHOD);
        }
        State bnt = Types.IntInstance.table;
        String[] num_methods_num = {
                "__abs__", "__add__", "__and__",
                "__class__", "__cmp__", "__coerce__", "__delattr__", "__div__",
                "__divmod__", "__doc__", "__float__", "__floordiv__",
                "__getattribute__", "__getnewargs__", "__hash__", "__hex__",
                "__index__", "__init__", "__int__", "__invert__", "__long__",
                "__lshift__", "__mod__", "__mul__", "__neg__", "__new__",
                "__nonzero__", "__oct__", "__or__", "__pos__", "__pow__",
                "__radd__", "__rand__", "__rdiv__", "__rdivmod__",
                "__reduce__", "__reduce_ex__", "__repr__", "__rfloordiv__",
                "__rlshift__", "__rmod__", "__rmul__", "__ror__", "__rpow__",
                "__rrshift__", "__rshift__", "__rsub__", "__rtruediv__",
                "__rxor__", "__setattr__", "__str__", "__sub__", "__truediv__",
                "__xor__"
        };
        for (String m : num_methods_num) {
            bnt.insert(m, numUrl(), newFunc(Types.IntInstance), METHOD);
        }
        bnt.insert("__getnewargs__", numUrl(), newFunc(newTuple(Types.IntInstance)), METHOD);
        bnt.insert("hex", numUrl(), newFunc(Types.StrInstance), METHOD);
        bnt.insert("conjugate", numUrl(), newFunc(Types.ComplexInstance), METHOD);

        State bct = Types.ComplexInstance.table;
        String[] complex_methods = {
                "__abs__", "__add__", "__div__", "__divmod__",
                "__float__", "__floordiv__", "__format__", "__getformat__", "__int__",
                "__long__", "__mod__", "__mul__", "__neg__", "__new__",
                "__pos__", "__pow__", "__radd__", "__rdiv__", "__rdivmod__",
                "__rfloordiv__", "__rmod__", "__rmul__", "__rpow__", "__rsub__",
                "__rtruediv__", "__sub__", "__truediv__", "conjugate"
        };
        for (String c : complex_methods) {
            bct.insert(c, numUrl(), newFunc(Types.ComplexInstance), METHOD);
        }
        String[] complex_methods_num = {
                "__eq__", "__ge__", "__gt__", "__le__", "__lt__", "__ne__",
                "__nonzero__", "__coerce__"
        };
        for (String cn : complex_methods_num) {
            bct.insert(cn, numUrl(), newFunc(Types.IntInstance), METHOD);
        }
        bct.insert("__getnewargs__", numUrl(), newFunc(newTuple(Types.ComplexInstance)), METHOD);
        bct.insert("imag", numUrl(), Types.IntInstance, ATTRIBUTE);
        bct.insert("real", numUrl(), Types.IntInstance, ATTRIBUTE);
    }


    void buildStrType() {
        Types.StrInstance.table.insert("__getslice__", newDataModelUrl("object.__getslice__"),
                                       newFunc(Types.StrInstance), METHOD);
        Types.StrInstance.table.insert("__getitem__", newDataModelUrl("object.__getitem__"),
                                       newFunc(Types.StrInstance), METHOD);
        Types.StrInstance.table.insert("__iter__", newDataModelUrl("object.__iter__"),
                                       newFunc(Types.StrInstance), METHOD);

        String[] str_methods_str = {
                "capitalize", "center", "decode", "encode", "expandtabs", "format",
                "index", "join", "ljust", "lower", "lstrip", "partition", "replace",
                "rfind", "rindex", "rjust", "rpartition", "rsplit", "rstrip",
                "strip", "swapcase", "title", "translate", "upper", "zfill"
        };
        for (String m : str_methods_str) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str." + m),
                                           newFunc(Types.StrInstance), METHOD);
        }

        String[] str_methods_num = {
                "count", "isalnum", "isalpha", "isdigit", "islower", "isspace",
                "istitle", "isupper", "find", "startswith", "endswith"
        };
        for (String m : str_methods_num) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str." + m),
                                           newFunc(Types.IntInstance), METHOD);
        }

        String[] str_methods_list = {"split", "splitlines"};
        for (String m : str_methods_list) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str." + m),
                                           newFunc(newList(Types.StrInstance)), METHOD);
        }
        Types.StrInstance.table.insert("partition", newLibUrl("stdtypes", "str.partition"),
                                       newFunc(newTuple(Types.StrInstance)), METHOD);
    }


    void buildModuleType() {
        String[] attrs = {"__doc__", "__file__", "__name__", "__package__"};
        for (String m : attrs) {
            BaseModule.table.insert(m, newTutUrl("modules.html"), Types.StrInstance, ATTRIBUTE);
        }
        BaseModule.table.insert("__dict__", newLibUrl("stdtypes", "modules"),
                                newDict(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE);
    }


    void buildDictType() {
        String url = "datastructures.html#dictionaries";
        State bt = Types.BaseDict.table;

        bt.insert("__getitem__", newTutUrl(url), newFunc(), METHOD);
        bt.insert("__iter__", newTutUrl(url), newFunc(), METHOD);
        bt.insert("get", newTutUrl(url), newFunc(), METHOD);

        bt.insert("items", newTutUrl(url),
                  newFunc(newList(newTuple(Types.UNKNOWN, Types.UNKNOWN))), METHOD);

        bt.insert("keys", newTutUrl(url), newFunc(BaseList), METHOD);
        bt.insert("values", newTutUrl(url), newFunc(BaseList), METHOD);

        String[] dict_method_unknown = {
                "clear", "copy", "fromkeys", "get", "iteritems", "iterkeys",
                "itervalues", "pop", "popitem", "setdefault", "update"
        };
        for (String m : dict_method_unknown) {
            bt.insert(m, newTutUrl(url), newFunc(), METHOD);
        }

        String[] dict_method_num = {"has_key"};
        for (String m : dict_method_num) {
            bt.insert(m, newTutUrl(url), newFunc(Types.IntInstance), METHOD);
        }
    }


    void buildFileType() {
        State table = BaseFile.table;

        table.insert("__enter__", newLibUrl("stdtypes", "contextmanager.__enter__"), newFunc(), METHOD);
        table.insert("__exit__", newLibUrl("stdtypes", "contextmanager.__exit__"), newFunc(), METHOD);
        table.insert("__iter__", newLibUrl("stdtypes", "iterator-types"), newFunc(), METHOD);

        String[] file_methods_unknown = {
            "__enter__", "__exit__", "__iter__", "flush", "readinto", "truncate"
        };
        for (String m : file_methods_unknown) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(), METHOD);
        }

        String[] methods_str = {"next", "read", "readline"};
        for (String m : methods_str) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(Types.StrInstance), METHOD);
        }

        String[] num = {"fileno", "isatty", "tell"};
        for (String m : num) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(Types.IntInstance), METHOD);
        }

        String[] methods_none = {"close", "seek", "write", "writelines"};
        for (String m : methods_none) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(Types.NoneInstance), METHOD);
        }

        table.insert("readlines", newLibUrl("stdtypes", "file.readlines"), newFunc(newList(Types.StrInstance)), METHOD);
        table.insert("xreadlines", newLibUrl("stdtypes", "file.xreadlines"), newFunc(Types.StrInstance), METHOD);
        table.insert("closed", newLibUrl("stdtypes", "file.closed"), Types.IntInstance, ATTRIBUTE);
        table.insert("encoding", newLibUrl("stdtypes", "file.encoding"), Types.StrInstance, ATTRIBUTE);
        table.insert("errors", newLibUrl("stdtypes", "file.errors"), Types.UNKNOWN, ATTRIBUTE);
        table.insert("mode", newLibUrl("stdtypes", "file.mode"), Types.IntInstance, ATTRIBUTE);
        table.insert("name", newLibUrl("stdtypes", "file.name"), Types.StrInstance, ATTRIBUTE);
        table.insert("softspace", newLibUrl("stdtypes", "file.softspace"), Types.IntInstance, ATTRIBUTE);
        table.insert("newlines", newLibUrl("stdtypes", "file.newlines"), newUnion(Types.StrInstance, newTuple(Types.StrInstance)), ATTRIBUTE);
    }


    void buildFunctionType() {
        State t = BaseFunction.table;

        for (String s : list("func_doc", "__doc__", "func_name", "__name__", "__module__")) {
            t.insert(s, new Url(DATAMODEL_URL), Types.StrInstance, ATTRIBUTE);
        }

        t.insert("func_closure", new Url(DATAMODEL_URL), newTuple(), ATTRIBUTE);
        t.insert("func_code", new Url(DATAMODEL_URL), Types.UNKNOWN, ATTRIBUTE);
        t.insert("func_defaults", new Url(DATAMODEL_URL), newTuple(), ATTRIBUTE);
        t.insert("func_globals", new Url(DATAMODEL_URL), new DictType(Types.StrInstance, Types.UNKNOWN),
                ATTRIBUTE);
        t.insert("func_dict", new Url(DATAMODEL_URL), new DictType(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE);

        // Assume any function can become a method, for simplicity.
        for (String s : list("__func__", "im_func")) {
            t.insert(s, new Url(DATAMODEL_URL), new FunType(), METHOD);
        }
    }


    // XXX:  finish wiring this up.  ClassType needs to inherit from it somehow,
    // so we can remove the per-instance attributes from NClassDef.
    void buildClassType() {
        State t = BaseClass.table;