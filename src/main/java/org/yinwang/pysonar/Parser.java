
package org.yinwang.pysonar;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.*;

import java.io.File;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class Parser {

    private static final String PYTHON2_EXE = "python";
    private static final String PYTHON3_EXE = "python3";
    private static final int TIMEOUT = 30000;

    Process python2Process;
    Process python3Process;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String dumpPythonResource = "org/yinwang/pysonar/python/dump_python.py";
    private String exchangeFile;
    private String endMark;
    private String jsonizer;
    private String parserLog;
    private String file;
    private String content;

    public Parser()
    {
        exchangeFile = $.getTempFile("json");
        endMark = $.getTempFile("end");
        jsonizer = $.getTempFile("dump_python");
        parserLog = $.getTempFile("parser_log");

        startPythonProcesses();
    }


    // start or restart python processes
    private void startPythonProcesses()
    {
        if (python2Process != null)
        {
            python2Process.destroy();
        }
        if (python3Process != null)
        {
            python3Process.destroy();
        }

        // copy dump_python.py to temp dir
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource(dumpPythonResource);
            FileUtils.copyURLToFile(url, new File(jsonizer));
        } catch (Exception e)
        {
            $.die("Failed to copy resource file:" + dumpPythonResource);
        }

        python2Process = startInterpreter(PYTHON2_EXE);
        if (python2Process != null)
        {
            $.msg("started: " + PYTHON2_EXE);
        }

        python3Process = startInterpreter(PYTHON3_EXE);
        if (python3Process != null)
        {
            $.msg("started: " + PYTHON3_EXE);
        }

        if (python2Process == null && python3Process == null)
        {
            $.die("You don't seem to have either of Python or Python3 on PATH");
        }
    }


    public void close() {
        if (python2Process != null)
        {
            python2Process.destroy();
        }

        if (python3Process != null)
        {
            python3Process.destroy();
        }

        if (!Analyzer.self.hasOption("debug")) {
            new File(exchangeFile).delete();
            new File(endMark).delete();
            new File(jsonizer).delete();
            new File(parserLog).delete();
        }
    }


    @Nullable
    public Node convert(Object o) {
        if (!(o instanceof Map)) {
            return null;
        }

        Map<String, Object> map = (Map<String, Object>) o;

        String type = (String) map.get("pysonar_node_type");
        Double startDouble = (Double) map.get("start");
        Double endDouble = (Double) map.get("end");
        Double lineDouble = (Double) map.get("lineno");
        Double colDouble = (Double) map.get("col_offset");

        int start = startDouble == null ? 0 : startDouble.intValue();
        int end = endDouble == null ? 1 : endDouble.intValue();
        int line = lineDouble == null ? 1 : lineDouble.intValue();
        int col = colDouble == null ? 1 : colDouble.intValue() + 1;


        if (type.equals("Module")) {
            Block b = convertBlock(map.get("body"));
            return new PyModule(b, file, start, end, line, col);
        }

        if (type.equals("alias")) {         // lower case alias
            String qname = (String) map.get("name");
            List<Name> names = segmentQname(qname, start + "import ".length(), false);
            Name asname = map.get("asname") == null ? null : new Name((String) map.get("asname"));
            return new Alias(names, asname, file, start, end, line, col);
        }

        if (type.equals("Assert")) {
            Node test = convert(map.get("test"));
            Node msg = convert(map.get("msg"));
            return new Assert(test, msg, file, start, end, line, col);
        }

        // assign could be x=y=z=1
        // turn it into one or more Assign nodes
        // z = 1; y = z; x = z
        if (type.equals("Assign")) {
            List<Node> targets = convertList(map.get("targets"));
            Node value = convert(map.get("value"));
            if (targets.size() == 1) {
                return new Assign(targets.get(0), value, file, start, end, line, col);
            } else {
                List<Node> assignments = new ArrayList<>();
                Node lastTarget = targets.get(targets.size() - 1);
                assignments.add(new Assign(lastTarget, value, file, start, end, line, col));

                for (int i = targets.size() - 2; i >= 0; i--) {
                    Node nextAssign = new Assign(targets.get(i), lastTarget, file, start, end, line, col);
                    assignments.add(nextAssign);
                }

                return new Block(assignments, file, start, end, line, col);
            }
        }

        if (type.equals("Attribute")) {
            Node value = convert(map.get("value"));
            Name attr = (Name) convert(map.get("attr_name"));
            if (attr == null) {
                attr = new Name((String) map.get("attr"));
            }
            return new Attribute(value, attr, file, start, end, line, col);
        }

        if (type.equals("AugAssign")) {
            Node target = convert(map.get("target"));
            Node value = convert(map.get("value"));
            Op op = convertOp(map.get("op"));
            Node operation = new BinOp(op, target, value, file, target.start, value.end, value.line, value.col);
            return new Assign(target, operation, file, start, end, line, col);
        }

        if (type.equals("BinOp")) {
            Node left = convert(map.get("left"));
            Node right = convert(map.get("right"));
            Op op = convertOp(map.get("op"));

            // desugar complex operators
            if (op == Op.NotEqual) {
                Node eq = new BinOp(Op.Equal, left, right, file, start, end, line, col);
                return new UnaryOp(Op.Not, eq, file, start, end, line, col);
            }

            if (op == Op.LtE) {
                Node lt = new BinOp(Op.Lt, left, right, file, start, end, line, col);
                Node eq = new BinOp(Op.Eq, left, right, file, start, end, line, col);
                return new BinOp(Op.Or, lt, eq, file, start, end, line, col);
            }

            if (op == Op.GtE) {
                Node gt = new BinOp(Op.Gt, left, right, file, start, end, line, col);
                Node eq = new BinOp(Op.Eq, left, right, file, start, end, line, col);
                return new BinOp(Op.Or, gt, eq, file, start, end, line, col);
            }

            if (op == Op.NotIn) {
                Node in = new BinOp(Op.In, left, right, file, start, end, line, col);
                return new UnaryOp(Op.Not, in, file, start, end, line, col);
            }

            if (op == Op.NotEq) {
                Node in = new BinOp(Op.Eq, left, right, file, start, end, line, col);
                return new UnaryOp(Op.Not, in, file, start, end, line, col);
            }

            return new BinOp(op, left, right, file, start, end, line, col);

        }

        if (type.equals("BoolOp")) {
            List<Node> values = convertList(map.get("values"));
            if (values == null || values.size() < 2) {
                $.die("impossible number of arguments, please fix the Python parser");
            }
            Op op = convertOp(map.get("op"));
            BinOp ret = new BinOp(op, values.get(0), values.get(1), file, start, end, line, col);
            for (int i = 2; i < values.size(); i++) {
                ret = new BinOp(op, ret, values.get(i), file, start, end, line, col);
            }
            return ret;
        }

        if (type.equals("Bytes")) {
            Object s = map.get("s");
            return new Bytes(s, file, start, end, line, col);
        }

        if (type.equals("Call")) {
            Node func = convert(map.get("func"));
            List<Node> args = convertList(map.get("args"));
            List<Keyword> keywords = convertList(map.get("keywords"));
            Node kwargs = convert(map.get("kwargs"));
            Node starargs = convert(map.get("starargs"));
            return new Call(func, args, keywords, kwargs, starargs, file, start, end, line, col);
        }

        if (type.equals("ClassDef")) {
            Name name = (Name) convert(map.get("name_node"));      // hack
            List<Node> bases = convertList(map.get("bases"));
            Block body = convertBlock(map.get("body"));
            return new ClassDef(name, bases, body, file, start, end, line, col);
        }

        // left-fold Compare into
        if (type.equals("Compare")) {
            Node left = convert(map.get("left"));
            List<Op> ops = convertListOp(map.get("ops"));
            List<Node> comparators = convertList(map.get("comparators"));
            Node result = new BinOp(ops.get(0), left, comparators.get(0), file, start, end, line, col);
            for (int i = 1; i < comparators.size(); i++) {
                Node compNext = new BinOp(ops.get(i), comparators.get(i - 1), comparators.get(i), file, start, end, line, col);
                result = new BinOp(Op.And, result, compNext, file, start, end, line, col);
            }
            return result;
        }

        if (type.equals("comprehension")) {
            Node target = convert(map.get("target"));
            Node iter = convert(map.get("iter"));
            List<Node> ifs = convertList(map.get("ifs"));
            return new Comprehension(target, iter, ifs, file, start, end, line, col);
        }

        if (type.equals("Break")) {
            return new Break(file, start, end, line, col);
        }

        if (type.equals("Continue")) {
            return new Continue(file, start, end, line, col);
        }

        if (type.equals("Delete")) {
            List<Node> targets = convertList(map.get("targets"));
            return new Delete(targets, file, start, end, line, col);
        }

        if (type.equals("Dict")) {
            List<Node> keys = convertList(map.get("keys"));
            List<Node> values = convertList(map.get("values"));
            return new Dict(keys, values, file, start, end, line, col);
        }

        if (type.equals("DictComp")) {
            Node key = convert(map.get("key"));
            Node value = convert(map.get("value"));
            List<Comprehension> generators = convertList(map.get("generators"));
            return new DictComp(key, value, generators, file, start, end, line, col);
        }

        if (type.equals("Ellipsis")) {
            return new Ellipsis(file, start, end, line, col);
        }

        if (type.equals("ExceptHandler")) {
            Node exception = convert(map.get("type"));
            List<Node> exceptions;

            if (exception != null) {
                exceptions = new ArrayList<>();
                exceptions.add(exception);
            } else {
                exceptions = null;
            }

            Node binder = convert(map.get("name"));
            Block body = convertBlock(map.get("body"));
            return new Handler(exceptions, binder, body, file, start, end, line, col);
        }

        if (type.equals("Exec")) {
            Node body = convert(map.get("body"));
            Node globals = convert(map.get("globals"));
            Node locals = convert(map.get("locals"));
            return new Exec(body, globals, locals, file, start, end, line, col);
        }

        if (type.equals("Expr")) {
            Node value = convert(map.get("value"));
            return new Expr(value, file, start, end, line, col);
        }

        if (type.equals("For") || type.equals("AsyncFor")) {
            Node target = convert(map.get("target"));
            Node iter = convert(map.get("iter"));
            Block body = convertBlock(map.get("body"));
            Block orelse = convertBlock(map.get("orelse"));
            return new For(target, iter, body, orelse, type.equals("AsyncFor"), file, start, end, line, col);
        }

        if (type.equals("FunctionDef") || type.equals("Lambda") || type.equals("AsyncFunctionDef")) {
            Name name = type.equals("Lambda") ? null : (Name) convert(map.get("name_node"));
            Map<String, Object> argsMap = (Map<String, Object>) map.get("args");
            List<Node> args = convertList(argsMap.get("args"));
            List<Node> defaults = convertList(argsMap.get("defaults"));
            Node body = type.equals("Lambda") ? convert(map.get("body")) : convertBlock(map.get("body"));

            // handle vararg depending on different python versions
            Name vararg = null;
            Object varargObj = argsMap.get("vararg");
            if (varargObj instanceof String) {
                vararg = new Name((String) argsMap.get("vararg"));
            } else if (varargObj instanceof Map) {
                String argName = (String) ((Map) varargObj).get("arg");
                vararg = new Name(argName);
            }

            // handle kwarg depending on different python versions
            Name kwarg = null;
            Object kwargObj = argsMap.get("kwarg");
            if (kwargObj instanceof String) {
                kwarg = new Name((String) argsMap.get("kwarg"));
            } else if (kwargObj instanceof Map) {
                String argName = (String) ((Map) kwargObj).get("arg");
                kwarg = new Name(argName);
            }

            boolean isAsync = type.equals("AsyncFunctionDef");

            List<Node> decors = new ArrayList<>();
            if (map.containsKey("decorator_list")) {
                decors = convertList(map.get("decorator_list"));
            }

            return new FunctionDef(name, args, body, defaults, vararg, kwarg, decors, file, isAsync, start, end, line, col);
        }

        if (type.equals("GeneratorExp")) {
            Node elt = convert(map.get("elt"));
            List<Comprehension> generators = convertList(map.get("generators"));
            return new GeneratorExp(elt, generators, file, start, end, line, col);
        }

        if (type.equals("Global")) {
            List<String> names = (List<String>) map.get("names");
            List<Name> nameNodes = new ArrayList<>();
            for (String name : names) {
                nameNodes.add(new Name(name));
            }
            return new Global(nameNodes, file, start, end, line, col);
        }

        if (type.equals("Nonlocal")) {
            List<String> names = (List<String>) map.get("names");
            List<Name> nameNodes = new ArrayList<>();
            for (String name : names) {
                nameNodes.add(new Name(name));
            }
            return new Global(nameNodes, file, start, end, line, col);
        }

        if (type.equals("If")) {
            Node test = convert(map.get("test"));
            Block body = convertBlock(map.get("body"));
            Block orelse = convertBlock(map.get("orelse"));
            return new If(test, body, orelse, file, start, end, line, col);
        }

        if (type.equals("IfExp")) {
            Node test = convert(map.get("test"));
            Node body = convert(map.get("body"));
            Node orelse = convert(map.get("orelse"));
            return new IfExp(test, body, orelse, file, start, end, line, col);
        }


        if (type.equals("Import")) {
            List<Alias> aliases = convertList(map.get("names"));
            locateNames(aliases, start);
            return new Import(aliases, file, start, end, line, col);
        }

        if (type.equals("ImportFrom")) {
            String module = (String) map.get("module");
            int level = ((Double) map.get("level")).intValue();
            List<Name> moduleSeg = module == null ? null : segmentQname(module, start + "from ".length() + level, true);
            List<Alias> names = convertList(map.get("names"));
            locateNames(names, start);
            return new ImportFrom(moduleSeg, names, level, file, start, end, line, col);
        }

        if (type.equals("Index")) {
            Node value = convert(map.get("value"));
            return new Index(value, file, start, end, line, col);
        }

        if (type.equals("keyword")) {
            String arg = (String) map.get("arg");
            Node value = convert(map.get("value"));
            return new Keyword(arg, value, file, start, end, line, col);
        }

        if (type.equals("List")) {
            List<Node> elts = convertList(map.get("elts"));
            return new PyList(elts, file, start, end, line, col);
        }

        if (type.equals("Starred")) { // f(*[1, 2, 3, 4])
            Node value = convert(map.get("value"));
            return new Starred(value, file, start, end, line, col);
        }

        if (type.equals("ListComp")) {
            Node elt = convert(map.get("elt"));
            List<Comprehension> generators = convertList(map.get("generators"));
            return new ListComp(elt, generators, file, start, end, line, col);
        }

        if (type.equals("Name")) {
            String id = (String) map.get("id");
            return new Name(id, file, start, end, line, col);
        }

        if (type.equals("NameConstant")) {
            String strVal;
            Object value = map.get("value");
            if (value == null) {
                strVal = "None";
            } else if (value instanceof Boolean) {
                strVal = ((Boolean) value) ? "True" : "False";
            } else if (value instanceof String) {
                strVal = (String) value;
            } else {
                $.msg("[Please report issue] NameConstant contains unrecognized value: " + value);
                strVal = "";
            }
            return new Name(strVal, file, start, end, line, col);
        }

        // Python3's new node type 'Constant'
        // Just convert to other types and call convert again
        if (type.equals("Constant")) {
            Object value = map.get("value");

            if (map.containsKey("num_type")) {
                map.put("pysonar_node_type", "Num");
                map.put("n", map.get("value"));
                return convert(map);
            }
            else if (value instanceof String) {
                map.put("pysonar_node_type", "Str");
                map.put("s", value);
                return convert(map);
            }
            else if (value instanceof Boolean) {
                map.put("pysonar_node_type", "NameConstant");
                return convert(map);
            }
            else {
                $.msg("\n[Please report issue]: Unexpected Constant node: " + type
                        + "\nnode: " + o + "\nfile: " + file);
                return new Unsupported(file, start, end, line, col);
            }
        }

        // another name for Name in Python3 func parameters?
        if (type.equals("arg")) {
            String id = (String) map.get("arg");
            return new Name(id, file, start, end, line, col);
        }

        if (type.equals("Num")) {

            String num_type = (String) map.get("num_type");
            if (num_type.equals("int")) {
                return new PyInt((String) map.get("n"), file, start, end, line, col);