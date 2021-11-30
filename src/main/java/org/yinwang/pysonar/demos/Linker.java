
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
        rootPath = root;
        outDir = outdir;
    }

    public void findLinks(@NotNull Analyzer analyzer) {
        $.msg("Adding xref links");
        Progress progress = new Progress(analyzer.getAllBindings().size(), 50);
        List<Binding> linkBindings = new ArrayList<>();

        for (Binding binding : analyzer.getAllBindings()) {
            if (binding.kind != Binding.Kind.MODULE) {
                linkBindings.add(binding);
            }
        }

        for (List<Binding> bs : $.correlateBindings(linkBindings)) {
            processDef(bs);
            progress.tick();
        }

        // highlight definitions
        $.msg("\nAdding ref links");
        progress = new Progress(analyzer.references.size(), 50);

        for (Node node: analyzer.references.keys()) {
            if (Analyzer.self.hasOption("debug")) {
                processRefDebug(node, analyzer.references.get(node));
            } else {
                processRef(node, analyzer.references.get(node));
            }
            progress.tick();
        }

        if (Analyzer.self.hasOption("report")) {
            for (Diagnostic d : analyzer.semanticErrors.values()) {
                processDiagnostic(d);
            }
        }
    }


    private void processDef(@NotNull List<Binding> bindings) {
        Binding first = bindings.get(0);
        String qname = first.qname;

        if (first.isURL() || first.start < 0) {
            return;
        }

        List<Type> types = bindings.stream().map(b -> b.type).collect(Collectors.toList());
        Style style = new Style(Style.Type.ANCHOR, first.start, first.end);
        style.message = UnionType.union(types).toString();
        style.url = first.qname;
        style.id = qname;
        addFileStyle(first.getFile(), style);
    }


    private void processDefDebug(@NotNull Binding binding) {
        int hash = binding.hashCode();

        if (binding.isURL() || binding.start < 0 || seenDef.contains(hash)) {
            return;
        }

        seenDef.add(hash);
        Style style = new Style(Style.Type.ANCHOR, binding.start, binding.end);
        style.message = binding.type.toString();
        style.url = binding.qname;
        style.id = "" + Math.abs(binding.hashCode());

        Set<Node> refs = binding.refs;
        style.highlight = new ArrayList<>();


        for (Node r : refs) {
            style.highlight.add(Integer.toString(Math.abs(r.hashCode())));
        }
        addFileStyle(binding.getFile(), style);
    }


    void processRef(@NotNull Node ref, @NotNull List<Binding> bindings) {
        String qname = bindings.iterator().next().qname;
        int hash = ref.hashCode();

        if (!seenRef.contains(hash)) {
            seenRef.add(hash);

            Style link = new Style(Style.Type.LINK, ref.start, ref.end);
            link.id = qname;

            List<Type> types = bindings.stream().map(b -> b.type).collect(Collectors.toList());
            link.message = UnionType.union(types).toString();

            // Currently jump to the first binding only. Should change to have a
            // hover menu or something later.
            String path = ref.file;
            if (path != null) {
                for (Binding b : bindings) {
                    if (link.url == null) {
                        link.url = toURL(b, path);
                    }

                    if (link.url != null) {
                        addFileStyle(path, link);
                        break;
                    }
                }
            }
        }
    }


    void processRefDebug(@NotNull Node ref, @NotNull List<Binding> bindings) {
        int hash = ref.hashCode();

        if (!seenRef.contains(hash)) {
            seenRef.add(hash);

            Style link = new Style(Style.Type.LINK, ref.start, ref.end);
            link.id = Integer.toString(Math.abs(hash));

            List<String> typings = new ArrayList<>();
            for (Binding b : bindings) {
                typings.add(b.type.toString());
            }
            link.message = $.joinWithSep(typings, " | ", "{", "}");

            link.highlight = new ArrayList<>();
            for (Binding b : bindings) {
                link.highlight.add(Integer.toString(Math.abs(b.hashCode())));
            }

            // Currently jump to the first binding only. Should change to have a
            // hover menu or something later.
            String path = ref.file;
            if (path != null) {
                for (Binding b : bindings) {
                    if (link.url == null) {
                        link.url = toURL(b, path);
                    }

                    if (link.url != null) {
                        addFileStyle(path, link);
                        break;
                    }
                }
            }
        }
    }


    /**
     * Returns the styles (links and extra styles) generated for a given file.
     *
     * @param path an absolute source path
     * @return a possibly-empty list of styles for that path