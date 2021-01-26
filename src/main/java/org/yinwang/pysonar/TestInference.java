
package org.yinwang.pysonar;

import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Dummy;
import org.yinwang.pysonar.ast.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestInference
{
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private String testFile;
    private String expecteRefsFile;
    private String missingRefsFile;
    private String wrongTypeFile;

    public TestInference(String testFile)
    {
        this.testFile = testFile;
        if (new File(testFile).isDirectory())
        {
            expecteRefsFile = $.makePathString(testFile, "refs.json");
            missingRefsFile = $.makePathString(testFile, "missing_refs");
            wrongTypeFile = $.makePathString(testFile, "wrong_types");
        }
        else
        {
            expecteRefsFile = $.makePathString(testFile + ".refs.json");
            missingRefsFile = $.makePathString(testFile + ".missing_refs");
            wrongTypeFile = $.makePathString(testFile + ".wrong_types");
        }
    }

    public Analyzer runAnalysis(String dir)
    {
        Map<String, Object> options = new HashMap<>();
        options.put("quiet", true);
        Analyzer analyzer = new Analyzer(options);
        analyzer.analyze(dir);

        analyzer.finish();
        return analyzer;
    }

    public void generateRefs(Analyzer analyzer)
    {
        List<Map<String, Object>> refs = new ArrayList<>();
        for (Node node: analyzer.references.keys())
        {
            String filename = node.file;
            List<Binding> bindings = analyzer.references.get(node);

            // only record those in the testFile
            if (filename != null && filename.startsWith(Analyzer.self.projectDir))
            {
                filename = $.projRelPath(filename).replaceAll("\\\\", "/");
                Map<String, Object> writeout = new LinkedHashMap<>();

                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("name", node.name);
                ref.put("file", filename);
                ref.put("start", node.start);
                ref.put("end", node.end);
                ref.put("line", node.line);
                ref.put("col", node.col);

                List<Map<String, Object>> dests = new ArrayList<>();
                Collections.sort(bindings, (a, b) -> a.start == b.start ? a.end - b.end : a.start - b.start);
                for (Binding b : bindings)
                {
                    String destFile = b.getFile();
                    if (destFile != null && destFile.startsWith(Analyzer.self.projectDir))
                    {
                        destFile = $.projRelPath(destFile).replaceAll("\\\\", "/");
                        Map<String, Object> dest = new LinkedHashMap<>();
                        dest.put("name", b.name);
                        dest.put("file", destFile);
                        dest.put("start", b.start);
                        dest.put("end", b.end);
                        dest.put("line", b.line);
                        dest.put("col", b.col);
                        dest.put("type", b.type.toString());
                        dests.add(dest);
                    }
                }
                if (!dests.isEmpty())
                {
                    writeout.put("ref", ref);
                    writeout.put("dests", dests);
                    refs.add(writeout);
                }