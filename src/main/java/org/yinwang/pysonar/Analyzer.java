
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
        this.modelDir = dest;

        try {
            $.copyResourcesRecursively(resource, new File(dest));
            $.msg("copied models to: " + modelDir);
        } catch (Exception e) {
            $.die("Failed to copy models. Please check permissions of writing to: " + dest);
        }
        addPath(dest);
    }


    @NotNull
    public List<String> getLoadPath() {
        List<String> loadPath = new ArrayList<>();
        if (cwd != null) {
            loadPath.add(cwd);
        }
        if (projectDir != null && (new File(projectDir).isDirectory())) {
            loadPath.add(projectDir);
        }
        loadPath.addAll(path);
        return loadPath;
    }


    public boolean inImportStack(Object f) {
        return importStack.contains(f);
    }


    public void pushImportStack(Object f) {
        importStack.add(f);
    }


    public void popImportStack(Object f) {
        importStack.remove(f);
    }


    @NotNull
    public List<Binding> getAllBindings() {
        return allBindings;
    }


    @Nullable
    ModuleType getCachedModule(String file) {
        Type t = moduleTable.lookupType($.moduleQname(file));
        if (t == null) {
            return null;
        } else if (t instanceof UnionType) {
            for (Type tt : ((UnionType) t).types) {
                if (tt instanceof ModuleType) {
                    return (ModuleType) tt;
                }
            }
            return null;
        } else if (t instanceof ModuleType) {
            return (ModuleType) t;
        } else {
            return null;
        }
    }


    public List<Diagnostic> getDiagnosticsForFile(String file) {
        List<Diagnostic> errs = semanticErrors.get(file);
        if (errs != null) {
            return errs;
        }
        return new ArrayList<>();
    }


    public void putRef(@NotNull Node node, @NotNull Collection<Binding> bs) {
        if (!(node instanceof Url)) {
            List<Binding> bindings = references.get(node);
            for (Binding b : bs) {
                if (!bindings.contains(b)) {
                    bindings.add(b);
                }
                b.addRef(node);
            }
        }
    }


    public void putRef(@NotNull Node node, @NotNull Binding b) {
        List<Binding> bs = new ArrayList<>();
        bs.add(b);
        putRef(node, bs);
    }


    public void putProblem(@NotNull Node loc, String msg) {
        String file = loc.file;
        if (file != null) {
            addFileErr(file, loc.start, loc.end, msg);
        }
    }


    // for situations without a Node
    public void putProblem(@Nullable String file, int begin, int end, String msg) {
        if (file != null) {
            addFileErr(file, begin, end, msg);
        }
    }


    void addFileErr(String file, int begin, int end, String msg) {
        Diagnostic d = new Diagnostic(file, Diagnostic.Category.ERROR, begin, end, msg);
        semanticErrors.put(file, d);
    }


    @Nullable
    public Type loadFile(String path) {
        path = $.unifyPath(path);
        File f = new File(path);

        if (!f.canRead()) {
            return null;
        }

        Type module = getCachedModule(path);
        if (module != null) {
            return module;
        }

        // detect circular import
        if (inImportStack(path)) {
            return null;
        }

        // set new CWD and save the old one on stack
        String oldcwd = cwd;
        setCWD(f.getParent());

        pushImportStack(path);
        Type type = parseAndResolve(path);
        popImportStack(path);

        // restore old CWD
        setCWD(oldcwd);
        return type;
    }


    @Nullable
    private Type parseAndResolve(String file) {
        loadingProgress.tick();
        Node ast = getAstForFile(file);

        if (ast == null) {
            failedToParse.add(file);
            return null;
        } else {
            Type type = inferencer.visit(ast, moduleTable);
            loadedFiles.add(file);
            return type;
        }
    }


    private String createCacheDir() {
        String dir = $.getTempFile("ast_cache");
        File f = new File(dir);
        $.msg("AST cache is at: " + dir);

        if (!f.exists()) {
            if (!f.mkdirs()) {
                $.die("Failed to create tmp directory: " + dir + ". Please check permissions");
            }
        }
        return dir;
    }


    /**
     * Returns the syntax tree for {@code file}. <p>
     */
    @Nullable
    public Node getAstForFile(String file) {
        return astCache.getAST(file);
    }


    @Nullable
    public ModuleType getBuiltinModule(@NotNull String qname) {
        return builtins.get(qname);
    }


    @Nullable