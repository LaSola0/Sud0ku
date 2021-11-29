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

        analyzer = new Analyzer(options);
        $.msg("Loading and analyzing files");
        try
        {
            analyzer.analyze(f.getPath());
        }
        finally
        {
            analyzer.finish();
        }

        generateHtml();
    }


    private void generateHtml() {
        $.msg("\nGenerating HTML");
        makeOutputDir();

        linker = new Linker(rootPath, OUTPUT_DIR);
        linker.findLinks(analyzer);

        int rootLength = rootPath.length();

        int total = 0;
        for (String path : analyzer.getLoadedFiles()) {
            if (path.startsWith(rootPath)) {
                total++;
            }
        }

        Progress progress = new Progress(total, 50);

        for (String path : analyzer.getLoadedFiles()) {
            if (path.startsWith(rootPath)) {
                progress.tick();
                File destFile = $.joinPath(OUTPUT_DIR, path.substring(rootLength));
                destFile.getParentFile().mkdirs();
                String destPath = destFile.getAbsolutePath() + ".html";
                String html = markup(path);
                try {
                    $.writeFile(destPath, html);
                } catch (Exception e) {
                    $.msg("Failed to write: " + destPath);
                }
            }
        }

        $.msg("\nWrote " + analyzer.getLoadedFiles().size() + " files to " + OUTPUT_DIR);
    }


    @NotNull
    private String markup(String path) {
        String source;

        try {
            source = $.readFile(path);
        } catch (Exception e) {
            $.die("Failed to read file: " + path);
            return "";
        }

        List<Style> styles = new ArrayList<>(linker.getStyles(path));

        String styledSource = new StyleApplier(path, source, styles).apply();
        String outline = new HtmlOutline(analyzer).generate(path);

        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n")
            .append("<head>\n")
            .append("<meta charset=\"utf-8\">\n")
            .append("<title>").append(path).append("</title>\n")
            .append("<style type='text/css'>\n").append(CSS).append("\n</style>\n")
            .append("<script language=\"JavaScript\" type=\"text/javascript\">\n")
            .append(Analyzer.self.hasOption("debug") ? JS_DEBUG : JS)
            .append("</script>\n")
            .append("</head>\n<body>\n")
            .append("<table width=100% border='1px solid gray'><tr><td valign='top'>")
            .append(outline)
            .append("</td><td>")
            .append("<pre>")
            .append(addLineNumbers(styledSource))
            .append("</pre>")
            .append("</td></tr></table></body></html>");
        return sb.toString();
    }


    @NotNull
    private String addLineNumbers(@NotNull String source) {
        StringBuilder result = new StringBuilder((int) (source.length() * 1.2));
        int count = 1;
        for (String line : source.split("\n")) {
            result.append("<span class='lineno'>");
            result.append(String.format("%1$4d", count++));
            result.append("</span> ");
            result.append(line);
            result.append("\n");
        }
        return result.toString();
    }


    private static void usage() {
        $.msg("Usage:  java -jar pysonar-2.