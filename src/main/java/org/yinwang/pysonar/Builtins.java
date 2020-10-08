
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

        for (String s : list("__name__", "__doc__", "__module__")) {
            t.insert(s, new Url(DATAMODEL_URL), Types.StrInstance, ATTRIBUTE);
        }

        t.insert("__dict__", new Url(DATAMODEL_URL), new DictType(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE);
    }


    class BuiltinsModule extends NativeModule {
        public BuiltinsModule() {
            super("__builtin__");
            Builtin = module = newModule(name);
            table = module.table;
        }


        @Nullable
        protected void addFunction(String name, Url url, Type type) {
            table.insert(name, url, newFunc(type), FUNCTION);
        }


        @Override
        public void initBindings() {
            Analyzer.self.moduleTable.insert(name, liburl(), module, MODULE);
            table.addSuper(BaseModule.table);

            addClass("object", newLibUrl("functions", "object"), Types.ObjectClass);
            addFunction("type", newLibUrl("functions", "type"), Types.TypeClass);

            addFunction("bool", newLibUrl("functions", "bool"), Types.BoolInstance);
            addClass("int", newLibUrl("functions", "int"), Types.IntClass);
            addClass("str", newLibUrl("functions", "func-str"), Types.StrClass);
            addClass("long", newLibUrl("functions", "long"), Types.LongClass);
            addClass("float", newLibUrl("functions", "float"), Types.FloatClass);
                addClass("complex", newLibUrl("functions", "complex"), Types.ComplexClass);

            addClass("None", newLibUrl("constants", "None"), Types.NoneInstance);

            addClass("dict", newLibUrl("stdtypes", "typesmapping"), Types.BaseDict);
            addFunction("file", newLibUrl("functions", "file"), BaseFileInst);
            addFunction("list", newLibUrl("functions", "list"), new InstanceType(BaseList));
            addFunction("tuple", newLibUrl("functions", "tuple"), new InstanceType(BaseTuple));

            // XXX:  need to model the following as built-in class types:
            //   basestring, bool, buffer, frozenset, property, set, slice,
            //   staticmethod, super and unicode
            String[] builtin_func_unknown = {
                    "apply", "basestring", "callable", "classmethod",
                    "coerce", "compile", "copyright", "credits", "delattr", "enumerate",
                    "eval", "execfile", "exit", "filter", "frozenset", "getattr",
                    "help", "input", "intern", "iter", "license", "long",
                    "property", "quit", "raw_input", "reduce", "reload", "reversed",
                    "set", "setattr", "slice", "sorted", "staticmethod", "super",
                    "type", "unichr", "unicode",
            };
            for (String f : builtin_func_unknown) {
                addFunction(f, newLibUrl("functions", f), Types.UNKNOWN);
            }

            String[] builtin_func_num = {
                    "abs", "all", "any", "cmp", "coerce", "divmod",
                    "hasattr", "hash", "id", "isinstance", "issubclass", "len", "max",
                    "min", "ord", "pow", "round", "sum"
            };
            for (String f : builtin_func_num) {
                addFunction(f, newLibUrl("functions", f), Types.IntInstance);
            }

            for (String f : list("hex", "oct", "repr", "chr")) {
                addFunction(f, newLibUrl("functions", f), Types.StrInstance);
            }

            addFunction("dir", newLibUrl("functions", "dir"), newList(Types.StrInstance));
            addFunction("map", newLibUrl("functions", "map"), newList(Types.UNKNOWN));
            addFunction("range", newLibUrl("functions", "range"), newList(Types.IntInstance));
            addFunction("xrange", newLibUrl("functions", "range"), newList(Types.IntInstance));
            addFunction("buffer", newLibUrl("functions", "buffer"), newList(Types.UNKNOWN));
            addFunction("zip", newLibUrl("functions", "zip"), newList(newTuple(Types.UNKNOWN)));


            for (String f : list("globals", "vars", "locals")) {
                addFunction(f, newLibUrl("functions.html#" + f), newDict(Types.StrInstance, Types.UNKNOWN));
            }

            for (String f : builtin_exception_types) {
                addClass(f, newLibUrl("exceptions", f),
                        newClass(f, Analyzer.self.globaltable, objectType));
            }
            BaseException = (ClassType) table.lookupType("BaseException");

            addAttr("True", newLibUrl("constants", "True"), Types.BoolInstance);
            addAttr("False", newLibUrl("constants", "False"), Types.BoolInstance);
            addAttr("None", newLibUrl("constants", "None"), Types.NoneInstance);
            addFunction("open", newTutUrl("inputoutput.html#reading-and-writing-files"), BaseFileInst);
            addFunction("__import__", newLibUrl("functions", "__import__"), newModule("<?>"));

            Analyzer.self.globaltable.insert("__builtins__", liburl(), module, ATTRIBUTE);
            Analyzer.self.globaltable.putAll(table);
        }
    }


    class ArrayModule extends NativeModule {
        public ArrayModule() {
            super("array");
        }


        @Override
        public void initBindings() {
            addClass("array", liburl("array.array"), BaseArray);
            addClass("ArrayType", liburl("array.ArrayType"), BaseArray);
        }
    }


    class AudioopModule extends NativeModule {
        public AudioopModule() {
            super("audioop");
        }


        @Override
        public void initBindings() {
            addClass(newException("error", table));

            addStrFuncs("add", "adpcm2lin", "alaw2lin", "bias", "lin2alaw", "lin2lin",
                    "lin2ulaw", "mul", "reverse", "tomono", "ulaw2lin");

            addNumFuncs("avg", "avgpp", "cross", "findfactor", "findmax",
                    "getsample", "max", "maxpp", "rms");

            for (String s : list("adpcm2lin", "findfit", "lin2adpcm", "minmax", "ratecv")) {
                addFunction(s, newTuple());
            }
        }
    }


    class BinasciiModule extends NativeModule {
        public BinasciiModule() {
            super("binascii");
        }


        @Override
        public void initBindings() {
            addStrFuncs(
                    "a2b_uu", "b2a_uu", "a2b_base64", "b2a_base64", "a2b_qp",
                    "b2a_qp", "a2b_hqx", "rledecode_hqx", "rlecode_hqx", "b2a_hqx",
                    "b2a_hex", "hexlify", "a2b_hex", "unhexlify");

            addNumFuncs("crc_hqx", "crc32");

            addClass(newException("Error", table));
            addClass(newException("Incomplete", table));
        }
    }


    class Bz2Module extends NativeModule {
        public Bz2Module() {
            super("bz2");
        }


        @Override
        public void initBindings() {
            ClassType bz2 = newClass("BZ2File", table, BaseFile);  // close enough.
            addClass(bz2);

            ClassType bz2c = newClass("BZ2Compressor", table, objectType);
            addMethod(bz2c, "compress", Types.StrInstance);
            addMethod(bz2c, "flush", Types.NoneInstance);
            addClass(bz2c);

            ClassType bz2d = newClass("BZ2Decompressor", table, objectType);
            addMethod(bz2d, "decompress", Types.StrInstance);
            addClass(bz2d);

            addFunction("compress", Types.StrInstance);
            addFunction("decompress", Types.StrInstance);
        }
    }


    class CPickleModule extends NativeModule {
        public CPickleModule() {
            super("cPickle");
        }


        @NotNull
        @Override
        protected Url liburl() {
            return newLibUrl("pickle", "module-cPickle");
        }


        @Override
        public void initBindings() {
            addUnknownFuncs("dump", "load", "dumps", "loads");

            addClass(newException("PickleError", table));

            ClassType picklingError = newException("PicklingError", table);
            addClass(picklingError);
            update("UnpickleableError", liburl(table.path + "." + "UnpickleableError"),
                    newClass("UnpickleableError", table, picklingError), CLASS);
            ClassType unpicklingError = newException("UnpicklingError", table);
            addClass(unpicklingError);
            update("BadPickleGet", liburl(table.path + "." + "BadPickleGet"),
                    newClass("BadPickleGet", table, unpicklingError), CLASS);

            ClassType pickler = newClass("Pickler", table, objectType);
            addMethod(pickler, "dump");
            addMethod(pickler, "clear_memo");
            addClass(pickler);

            ClassType unpickler = newClass("Unpickler", table, objectType);
            addMethod(unpickler, "load");
            addMethod(unpickler, "noload");
            addClass(unpickler);
        }
    }


    class CStringIOModule extends NativeModule {
        public CStringIOModule() {
            super("cStringIO");
        }


        @NotNull
        @Override
        protected Url liburl() {
            return newLibUrl("stringio");
        }


        @NotNull
        @Override
        protected Url liburl(String anchor) {
            return newLibUrl("stringio", anchor);
        }

        @Override
        public void initBindings() {
            ClassType StringIO = newClass("StringIO", table, BaseFile);
            addFunction("StringIO", new InstanceType(StringIO));
            addAttr("InputType", BaseType);
            addAttr("OutputType", BaseType);
            addAttr("cStringIO_CAPI", Types.UNKNOWN);
        }
    }


    class CMathModule extends NativeModule {
        public CMathModule() {
            super("cmath");
        }


        @Override
        public void initBindings() {
            addFunction("phase", Types.IntInstance);
            addFunction("polar", newTuple(Types.IntInstance, Types.IntInstance));
            addFunction("rect", Types.ComplexInstance);

            for (String plf : list("exp", "log", "log10", "sqrt")) {
                addFunction(plf, Types.IntInstance);
            }

            for (String tf : list("acos", "asin", "atan", "cos", "sin", "tan")) {
                addFunction(tf, Types.IntInstance);
            }

            for (String hf : list("acosh", "asinh", "atanh", "cosh", "sinh", "tanh")) {
                addFunction(hf, Types.ComplexInstance);
            }

            for (String cf : list("isinf", "isnan")) {
                addFunction(cf, Types.BoolInstance);
            }

            for (String c : list("pi", "e")) {
                addAttr(c, Types.IntInstance);
            }
        }
    }


    class CollectionsModule extends NativeModule {
        public CollectionsModule() {
            super("collections");
        }


        @NotNull
        private Url abcUrl() {
            return liburl("abcs-abstract-base-classes");
        }


        @NotNull
        private Url dequeUrl() {
            return liburl("deque-objects");
        }


        @Override
        public void initBindings() {
            ClassType callable = newClass("Callable", table, objectType);
            callable.table.insert("__call__", abcUrl(), newFunc(), METHOD);
            addClass(callable);

            ClassType iterableType = newClass("Iterable", table, objectType);
            // TODO should this jump to url like https://docs.python.org/2.7/library/stdtypes.html#iterator.__iter__ ?
            iterableType.table.insert("__next__", abcUrl(), newFunc(), METHOD);
            iterableType.table.insert("__iter__", abcUrl(), newFunc(), METHOD);
            addClass(iterableType);

            ClassType Hashable = newClass("Hashable", table, objectType);
            Hashable.table.insert("__hash__", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(Hashable);

            ClassType Sized = newClass("Sized", table, objectType);
            Sized.table.insert("__len__", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(Sized);

            ClassType containerType = newClass("Container", table, objectType);
            containerType.table.insert("__contains__", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(containerType);

            ClassType iteratorType = newClass("Iterator", table, iterableType);
            addClass(iteratorType);

            ClassType sequenceType = newClass("Sequence", table, Sized, iterableType, containerType);
            sequenceType.table.insert("__getitem__", abcUrl(), newFunc(), METHOD);
            sequenceType.table.insert("reversed", abcUrl(), newFunc(sequenceType), METHOD);
            sequenceType.table.insert("index", abcUrl(), newFunc(Types.IntInstance), METHOD);
            sequenceType.table.insert("count", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(sequenceType);

            ClassType mutableSequence = newClass("MutableSequence", table, sequenceType);
            mutableSequence.table.insert("__setitem__", abcUrl(), newFunc(), METHOD);
            mutableSequence.table.insert("__delitem__", abcUrl(), newFunc(), METHOD);
            addClass(mutableSequence);

            ClassType setType = newClass("Set", table, Sized, iterableType, containerType);
            setType.table.insert("__getitem__", abcUrl(), newFunc(), METHOD);
            addClass(setType);

            ClassType mutableSet = newClass("MutableSet", table, setType);
            mutableSet.table.insert("add", abcUrl(), newFunc(), METHOD);
            mutableSet.table.insert("discard", abcUrl(), newFunc(), METHOD);
            addClass(mutableSet);

            ClassType mapping = newClass("Mapping", table, Sized, iterableType, containerType);
            mapping.table.insert("__getitem__", abcUrl(), newFunc(), METHOD);
            addClass(mapping);

            ClassType mutableMapping = newClass("MutableMapping", table, mapping);
            mutableMapping.table.insert("__setitem__", abcUrl(), newFunc(), METHOD);
            mutableMapping.table.insert("__delitem__", abcUrl(), newFunc(), METHOD);
            addClass(mutableMapping);

            ClassType MappingView = newClass("MappingView", table, Sized);
            addClass(MappingView);

            ClassType KeysView = newClass("KeysView", table, Sized);
            addClass(KeysView);

            ClassType ItemsView = newClass("ItemsView", table, Sized);
            addClass(ItemsView);

            ClassType ValuesView = newClass("ValuesView", table, Sized);
            addClass(ValuesView);

            ClassType deque = newClass("deque", table, objectType);
            for (String n : list("append", "appendLeft", "clear",
                    "extend", "extendLeft", "rotate"))
            {
                deque.table.insert(n, dequeUrl(), newFunc(Types.NoneInstance), METHOD);
            }
            for (String u : list("__getitem__", "__iter__",
                    "pop", "popleft", "remove"))
            {
                deque.table.insert(u, dequeUrl(), newFunc(), METHOD);
            }
            addClass(deque);

            ClassType defaultdict = newClass("defaultdict", table, objectType);
            defaultdict.table.insert("__missing__", liburl("defaultdict-objects"),
                    newFunc(), METHOD);
            defaultdict.table.insert("default_factory", liburl("defaultdict-objects"),
                    newFunc(), METHOD);
            addClass(defaultdict);

            String argh = "namedtuple-factory-function-for-tuples-with-named-fields";
            ClassType namedtuple = newClass("(namedtuple)", table, BaseTuple);
            namedtuple.table.insert("_fields", liburl(argh),
                                    new ListType(Types.StrInstance), ATTRIBUTE);
            addFunction("namedtuple", namedtuple);
        }
    }


    class CTypesModule extends NativeModule {
        public CTypesModule() {
            super("ctypes");
        }


        @Override
        public void initBindings() {
            String[] ctypes_attrs = {
                    "ARRAY", "ArgumentError", "Array", "BigEndianStructure", "CDLL",
                    "CFUNCTYPE", "DEFAULT_MODE", "DllCanUnloadNow", "DllGetClassObject",
                    "FormatError", "GetLastError", "HRESULT", "LibraryLoader",
                    "LittleEndianStructure", "OleDLL", "POINTER", "PYFUNCTYPE", "PyDLL",
                    "RTLD_GLOBAL", "RTLD_LOCAL", "SetPointerType", "Structure", "Union",
                    "WINFUNCTYPE", "WinDLL", "WinError", "_CFuncPtr", "_FUNCFLAG_CDECL",
                    "_FUNCFLAG_PYTHONAPI", "_FUNCFLAG_STDCALL", "_FUNCFLAG_USE_ERRNO",
                    "_FUNCFLAG_USE_LASTERROR", "_Pointer", "_SimpleCData",
                    "_c_functype_cache", "_calcsize", "_cast", "_cast_addr",
                    "_check_HRESULT", "_check_size", "_ctypes_version", "_dlopen",
                    "_endian", "_memmove_addr", "_memset_addr", "_os",
                    "_pointer_type_cache", "_string_at", "_string_at_addr", "_sys",
                    "_win_functype_cache", "_wstring_at", "_wstring_at_addr",
                    "addressof", "alignment", "byref", "c_bool", "c_buffer", "c_byte",
                    "c_char", "c_char_p", "c_double", "c_float", "c_int", "c_int16",
                    "c_int32", "c_int64", "c_int8", "c_long", "c_longdouble",
                    "c_longlong", "c_short", "c_size_t", "c_ubyte", "c_uint",
                    "c_uint16", "c_uint32", "c_uint64", "c_uint8", "c_ulong",
                    "c_ulonglong", "c_ushort", "c_void_p", "c_voidp", "c_wchar",
                    "c_wchar_p", "cast", "cdll", "create_string_buffer",
                    "create_unicode_buffer", "get_errno", "get_last_error", "memmove",
                    "memset", "oledll", "pointer", "py_object", "pydll", "pythonapi",
                    "resize", "set_conversion_mode", "set_errno", "set_last_error",
                    "sizeof", "string_at", "windll", "wstring_at"
            };
            for (String attr : ctypes_attrs) {
                addAttr(attr, Types.UNKNOWN);
            }
        }
    }


    class CryptModule extends NativeModule {
        public CryptModule() {
            super("crypt");
        }


        @Override
        public void initBindings() {
            addStrFuncs("crypt");
        }
    }


    class DatetimeModule extends NativeModule {
        public DatetimeModule() {
            super("datetime");
        }


        @NotNull
        private Url dtUrl(String anchor) {
            return liburl("datetime." + anchor);
        }


        @Override
        public void initBindings() {
            // XXX:  make datetime, time, date, timedelta and tzinfo Base* objects,
            // so built-in functions can return them.

            addNumAttrs("MINYEAR", "MAXYEAR");

            ClassType timedelta = Datetime_timedelta = newClass("timedelta", table, objectType);
            addClass(timedelta);
            addAttr(timedelta, "min", timedelta);
            addAttr(timedelta, "max", timedelta);
            addAttr(timedelta, "resolution", timedelta);
            addAttr(timedelta, "days", Types.IntInstance);
            addAttr(timedelta, "seconds", Types.IntInstance);
            addAttr(timedelta, "microseconds", Types.IntInstance);

            ClassType tzinfo = Datetime_tzinfo = newClass("tzinfo", table, objectType);
            addClass(tzinfo);
            addMethod(tzinfo, "utcoffset", timedelta);
            addMethod(tzinfo, "dst", timedelta);
            addMethod(tzinfo, "tzname", Types.StrInstance);
            addMethod(tzinfo, "fromutc", tzinfo);

            ClassType date = Datetime_date = newClass("date", table, objectType);
            addClass(date);
            addAttr(date, "min", date);
            addAttr(date, "max", date);
            addAttr(date, "resolution", timedelta);

            addMethod(date, "today", date);
            addMethod(date, "fromtimestamp", date);
            addMethod(date, "fromordinal", date);

            addAttr(date, "year", Types.IntInstance);
            addAttr(date, "month", Types.IntInstance);
            addAttr(date, "day", Types.IntInstance);

            addMethod(date, "replace", date);
            addMethod(date, "timetuple", Time_struct_time);

            for (String n : list("toordinal", "weekday", "isoweekday")) {
                addMethod(date, n, Types.IntInstance);
            }
            for (String r : list("ctime", "strftime", "isoformat")) {
                addMethod(date, r, Types.StrInstance);
            }
            addMethod(date, "isocalendar", newTuple(Types.IntInstance, Types.IntInstance, Types.IntInstance));

            ClassType time = Datetime_time = newClass("time", table, objectType);
            addClass(time);