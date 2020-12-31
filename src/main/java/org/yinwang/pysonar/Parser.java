
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