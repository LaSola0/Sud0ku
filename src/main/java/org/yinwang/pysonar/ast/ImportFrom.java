package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ImportFrom extends Node {

    public List<Name> module;
    public List<Alias> names;
    public int level;

    public ImportFrom(List<Name> module, List<Alias> names, int level, String file, int start, int end, int line, int col) {
        super(NodeType.IMPORTFROM, file, start, end, line, col);
        this.module = module;
        this.level = level;
        this.names = names;
        addChildren(names);
    }

    public boolean isImportStar() {
        return names.size() == 1 && "*".equals(names.get(0).name.get(0).id);
    }

    public void importStar(@NotNull State s, @Nullable Type mt) {
        if (mt == null || mt.file == null) {
            return;
        }

        Node node = Analyzer.self.getAstForFile(mt.file);
        if (node == null) {
            return;
        }

        List<String> names = new ArrayList<>();
        Type allType = mt.table.lookupType("__all__");

        if (allType != null && allType instanceof ListType) {
            ListType 