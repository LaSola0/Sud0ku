package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.$;

public class ModuleType extends Type {

    @NotNull
    public String name;
    @Nullable
    public String qname;


    public ModuleType(@NotNull String name, @Nullable String file, @NotNull State parent) {
        this.name = name;
        this.file = file;  // null for builtin modules
        if (file != null) {
            // This 