
package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.*;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Collects per-file hyperlinks, as well as styles that require the
 * symbol table to resolve properly.
 */
class Linker {

    private static final Pattern CONSTANT = Pattern.compile("[A-Z_][A-Z0-9_]*");

    // Map of file-path to semantic styles & links for that path.
    @NotNull
    private Map<String, List<Style>> fileStyles = new HashMap<>();

    private File outDir;  // where we're generating the output html
    private String rootPath;

    // prevent duplication in def and ref links
    Set<Object> seenDef = new HashSet<>();
    Set<Object> seenRef = new HashSet<>();


    /**
     * Constructor.
     *
     * @param root   the root of the directory tree being indexed
     * @param outdir the html output directory
     */
    public Linker(String root, File outdir) {