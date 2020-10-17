package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;


public class Diagnostic {
    public enum Category {
        INFO, WARNING, ERROR
    }


    public String file;
    public Category category;
    public int start;
    public int end;
    public String msg;


    public Diagnostic(String file, 