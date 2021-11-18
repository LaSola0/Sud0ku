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
    private static final String JS = $.readResource("org/yinwang