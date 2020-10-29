package org.yinwang.pysonar;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import org.yinwang.pysonar.ast.FunctionDef;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.ast.Str;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JSONDump {

    private static Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static Set<String> seenDef = new HashSet<>();
    private static Set<String> seenRef = new HashSet<>();
    private static Set<String> seenDocs = new HashSet<>();


    private static String dirname(String path) {
        return new File(path).getParent();
    }


    private static Analyzer newAnalyzer(String srcpath, String[] inclpaths) throws Exception {
        Analyzer idx = new Analyzer();
        for (String inclpath : inclpaths) {
            idx.addPath(inclpath);
        }

        idx.analyze(srcpath);
        idx.finish();

        if (idx.semanticErrors.size() > 0) {
            log.info("Analyzer errors:");
            for (String file : idx.semanticErrors.keys()) {
                log.info("  File: " + file);
                Collection<Diagnostic> diagnostics = idx.semanticErrors.get(file);
                for (Diagnostic d : diagnostics) {
                    log.info("    " + d);
                }
            }
        }

        return idx;
    }


    private static void writeSymJson(Binding binding, JsonGenerator json) throws IOException {
        if (binding.start < 0) {
            return;
        }

        String name = binding.name;
        boolean isExported = !(
                Binding.Kind.VARIABLE == binding.kind ||
                        Binding.Kind.PARAMETER == binding.kind ||
                        Binding.Kind.SCOPE == binding.kind ||
                        Binding.Kind.ATTRIBUTE == binding.kind ||
                        (name.length() == 0 || name.charAt(0) == '_' || name.startsWith("lambda%")));

        String path = binding.qname.replace('.', '/').replace("%20", ".");

        if (!seenDef.contains(path)) {
            seenDef.add(path);
            json.writeStartObject();
            json.writeStr