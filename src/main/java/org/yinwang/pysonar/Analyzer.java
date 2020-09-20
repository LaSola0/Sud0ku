
package org.yinwang.pysonar;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Name;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.ast.Url;
import org.yinwang.pysonar.types.*;
import org.yinwang.pysonar.visitor.TypeInferencer;

import java.io.File;
import java.net.URL;
import java.util.*;


public class Analyzer {

    // global static instance of the analyzer itself
    public static Analyzer self;
    public TypeInferencer inferencer = new TypeInferencer();
    public String sid = $.newSessionId();
    public State moduleTable = new State(null, State.StateType.GLOBAL);
    public List<String> loadedFiles = new ArrayList<>();
    public State globaltable = new State(null, State.StateType.GLOBAL);
    public List<Binding> allBindings = new ArrayList<>();
    public ListMultimap<Node, Binding> references = ArrayListMultimap.create();
    public Set<Name> resolved = new HashSet<>();
    public Set<Name> unresolved = new HashSet<>();
    public ListMultimap<String, Diagnostic> semanticErrors = ArrayListMultimap.create();
    public String cwd = null;
    public int nCalled = 0;
    public boolean multilineFunType = false;
    public List<String> path = new ArrayList<>();
    private Set<FunType> uncalled = new HashSet<>();
    private Set<Object> importStack = new HashSet<>();

    private AstCache astCache;
    public String cacheDir;
    public Set<String> failedToParse = new HashSet<>();
    public Stats stats = new Stats();
    public Builtins builtins;
    private Progress loadingProgress = null;

    public String projectDir;
    public String modelDir;
    public Stack<CallStackEntry> callStack = new Stack<>();

    public Map<String, Object> options;


    public Analyzer() {
        this(null);
    }


    public Analyzer(Map<String, Object> options) {
        self = this;
        if (options != null) {
            this.options = options;
        } else {
            this.options = new HashMap<>();
        }
        this.stats.putInt("startTime", System.currentTimeMillis());
        this.builtins = new Builtins();
        this.builtins.init();
        this.cacheDir = createCacheDir();
        this.astCache = new AstCache();
        addPythonPath();
        copyModels();
    }


    public boolean hasOption(String option) {
        Object op = options.get(option);
        if (op != null && op.equals(true)) {
            return true;
        } else {
            return false;
        }
    }


    public void setOption(String option) {
        options.put(option, true);
    }


    // main entry to the analyzer
    public void analyze(String path) {
        String upath = $.unifyPath(path);
        File f = new File(upath);
        projectDir = f.isDirectory() ? f.getPath() : f.getParent();
        loadFileRecursive(upath);
    }


    public void setCWD(String cd) {
        if (cd != null) {
            cwd = $.unifyPath(cd);
        }
    }


    public void addPaths(@NotNull List<String> p) {
        for (String s : p) {
            addPath(s);
        }
    }


    public void addPath(String p) {
        path.add($.unifyPath(p));
    }


    public void setPath(@NotNull List<String> path) {
        this.path = new ArrayList<>(path.size());
        addPaths(path);
    }


    private void addPythonPath() {
        String path = System.getenv("PYTHONPATH");
        if (path != null) {
            String[] segments = path.split(":");
            for (String p : segments) {
                addPath(p);
            }
        }
    }


    private void copyModels() {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(Globals.MODEL_LOCATION);
        String dest = $.getTempFile("models");