package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.$;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Options;
import org.yinwang.pysonar.Progress;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Demo {

    private static File OUTPUT_DIR;

    private static final String CSS = $.readResource("org/yinwang/pysonar/css/demo.css");
    private static final String JS = $.readResource("org/yinwang/pysonar/javascript/highlight.js");
    private static final String JS_DEBUG = $.readResource("org/yinwang/pysonar/javascript/highlight-debug.js");

    private Analyzer analyzer;
    private String rootPath;
    private Linker linker;


    private void makeOutputDir() {
        if (!OUTPUT_DIR.exists()) {
            OUTPUT_DIR.mkdirs();
            $.msg("Created directory: " + OUTPUT_DIR.getAbsolutePath());
        }
    }

    private void start(@NotNull String fileOrDir, Map<String, Object> options) throws Exception
    {
        File f = new File(fileOrDir);
        File rootDir = f.isFile() ? f.getParentFile() : f;
        try
        {
            rootPath = $.unifyPath(rootDir);
        }
        catch (Exception e)
        {
            $.die("File not found: " + f);
        }

        analyzer = new Analyzer(