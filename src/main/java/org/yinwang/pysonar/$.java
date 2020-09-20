
package org.yinwang.pysonar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * unsorted utility class
 */
public class $ {

    public static String baseFileName(String filename) {
        return new File(filename).getName();
    }


    public static String hashFileName(String filename) {
        return Integer.toString(filename.hashCode());
    }


    public static boolean same(@Nullable Object o1, @Nullable Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }

    public static String getTempFile(String file)
    {
        String tmpDir = getTempDir();
        return makePathString(tmpDir, file);
    }

    public static String getTempDir()
    {
        String systemTemp = getSystemTempDir();
        return makePathString(systemTemp, "pysonar2-" + Analyzer.self.sid);
    }

    public static String getSystemTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        String sep = System.getProperty("file.separator");
        if (tmp.endsWith(sep)) {
            return tmp;
        }
        return tmp + sep;
    }


    /**
     * Returns the parent qname of {@code qname} -- everything up to the
     * last dot (exclusive), or if there are no dots, the empty string.
     */
    public static String getQnameParent(@Nullable String qname) {
        if (qname == null || qname.isEmpty()) {
            return "";
        }
        int index = qname.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return qname.substring(0, index);
    }


    @Nullable
    public static String moduleQname(@NotNull String file) {
        File f = new File(file);

        if (f.getName().endsWith("__init__.py")) {
            file = f.getParent();
        } else if (file.endsWith(Globals.FILE_SUFFIX)) {
            file = file.substring(0, file.length() - Globals.FILE_SUFFIX.length());
        }

        // remove Windows like '\\' and 'C:'
        file = file.replaceAll("^\\\\", "");
        file = file.replaceAll("^[a-zA-Z]:", "");

        return file.replace(".", "%20").replace('/', '.').replace('\\', '.');
    }


    /**
     * Given an absolute {@code path} to a file (not a directory),
     * returns the module name for the file.  If the file is an __init__.py,
     * returns the last component of the file's parent directory, else
     * returns the filename without path or extension.
     */
    public static String moduleName(String path) {
        File f = new File(path);
        String name = f.getName();
        if (name.equals("__init__.py")) {
            return f.getParentFile().getName();
        } else if (name.endsWith(Globals.FILE_SUFFIX)) {
            return name.substring(0, name.length() - Globals.FILE_SUFFIX.length());
        } else {
            return name;
        }
    }


    @NotNull
    public static String arrayToString(@NotNull Collection<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }


    @NotNull
    public static String arrayToSortedStringSet(Collection<String> strings) {
        Set<String> sorter = new TreeSet<>(strings);
        return arrayToString(sorter);
    }


    public static void writeFile(String path, String contents) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
            out.print(contents);
            out.flush();
        } catch (Exception e) {
            $.die("Failed to write: " + path);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }


    @Nullable
    public static String readFile(@NotNull String path) {
        // Don't use line-oriented file read -- need to retain CRLF if present
        // so the style-run and link offsets are correct.
        byte[] content = getBytesFromFile(path);
        if (content == null) {
            return null;
        } else {
            return new String(content, Charset.forName("UTF-8"));
        }
    }


    @Nullable
    public static byte[] getBytesFromFile(@NotNull String filename) {
        try {
            return FileUtils.readFileToByteArray(new File(filename));
        } catch (Exception e) {
            return null;
        }
    }


    static boolean isReadableFile(String path) {
        File f = new File(path);
        return f.canRead() && f.isFile();
    }


    @NotNull
    public static String readWhole(@NotNull InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[8192];

        int nRead;
        while ((nRead = is.read(bytes, 0, 8192)) > 0) {
            sb.append(new String(bytes, 0, nRead));
        }
        return sb.toString();
    }


    public static void copyResourcesRecursively(URL originUrl, File destination) throws Exception {
        URLConnection urlConnection = originUrl.openConnection();

        if (urlConnection.getURL().getProtocol().equals("jar")) {
            copyJarResourcesRecursively(destination, (JarURLConnection) urlConnection);
        } else if (urlConnection.getURL().getProtocol().equals("file")) {
            FileUtils.copyDirectory(new File(originUrl.getPath()), destination);
        } else {
            die("Unsupported URL type: " + urlConnection);
        }
    }


    public static void copyJarResourcesRecursively(File destination, JarURLConnection jarConnection) {
        JarFile jarFile;
        try {
            jarFile = jarConnection.getJarFile();
        } catch (Exception e) {
            $.die("Failed to get jar file)");
            return;
        }

        Enumeration<JarEntry> em = jarFile.entries();
        while (em.hasMoreElements()) {
            JarEntry entry = em.nextElement();
            if (entry.getName().startsWith(jarConnection.getEntryName())) {
                String fileName = StringUtils.removeStart(entry.getName(), jarConnection.getEntryName());
                if (!fileName.equals("/")) {  // exclude the directory
                    InputStream entryInputStream = null;
                    try {
                        entryInputStream = jarFile.getInputStream(entry);
                        FileUtils.copyInputStreamToFile(entryInputStream, new File(destination, fileName));
                    } catch (Exception e) {
                        die("Failed to copy resource: " + fileName, e);
                    } finally {
                        if (entryInputStream != null) {
                            try {
                                entryInputStream.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
    }


    public static String readResource(String resource) {
        InputStream s = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        return readWholeStream(s);
    }


    /**
     * get unique hash according to file content and filename
     */
    @NotNull
    public static String getFileHash(@NotNull String path) {
        byte[] bytes = getBytesFromFile(path);
        return $.getContentHash(path.getBytes()) + "." + getContentHash(bytes);
    }


    @NotNull
    public static String getContentHash(byte[] fileContents) {
        MessageDigest algorithm;

        try {
            algorithm = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            $.die("Failed to get SHA, shouldn't happen");
            return "";
        }

        algorithm.reset();
        algorithm.update(fileContents);
        byte[] messageDigest = algorithm.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aMessageDigest : messageDigest) {
            sb.append(String.format("%02x", 0xFF & aMessageDigest));
        }
        return sb.toString();
    }


    public static String escapeQname(@NotNull String s) {
        return s.replaceAll("[.&@%-]", "_");
    }


    public static String escapeWindowsPath(String path) {
        return path.replace("\\", "\\\\");
    }


    @NotNull
    public static Collection<String> toStringCollection(@NotNull Collection<Integer> collection) {
        List<String> ret = new ArrayList<>();
        for (Integer x : collection) {
            ret.add(x.toString());
        }
        return ret;
    }


    @NotNull
    public static String joinWithSep(@NotNull Collection<String> ls, String sep, @Nullable String start,
                                     @Nullable String end)
    {
        StringBuilder sb = new StringBuilder();
        if (start != null && ls.size() > 1) {
            sb.append(start);
        }
        int i = 0;
        for (String s : ls) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(s);
            i++;
        }
        if (end != null && ls.size() > 1) {
            sb.append(end);
        }
        return sb.toString();
    }


    public static void msg(String m) {
        if (Analyzer.self != null && !Analyzer.self.hasOption("quiet")) {
            System.out.println(m);
        }
    }


    public static void msg_(String m) {
        if (Analyzer.self != null && !Analyzer.self.hasOption("quiet")) {
            System.out.print(m);
        }
    }


    public static void testmsg(String m) {
        System.out.println(m);
    }


    public static void die(String msg) {
        die(msg, null);
    }


    public static void die(String msg, Exception e) {
        System.err.println(msg);

        if (e != null) {
            System.err.println("Exception: " + e + "\n");
        }

        Thread.dumpStack();
        System.exit(2);
    }


    @Nullable
    public static String readWholeFile(String filename) {
        try {
            return new Scanner(new File(filename)).useDelimiter("PYSONAR2END").next();
        } catch (FileNotFoundException e) {
            return null;
        }
    }


    public static String readWholeStream(InputStream in) {
        return new Scanner(in).useDelimiter("\\Z").next();
    }


    @NotNull
    public static String percent(long num, long total) {
        if (total == 0) {
            return "100%";
        } else {
            int pct = (int) (num * 100 / total);
            return String.format("%1$3d", pct) + "%";
        }
    }


    @NotNull
    public static String formatTime(long millis) {
        long sec = millis / 1000;
        long min = sec / 60;
        sec = sec % 60;
        long hr = min / 60;
        min = min % 60;

        return hr + ":" + min + ":" + sec;
    }


    /**
     * format number with fixed width
     */
    public static String formatNumber(Object n, int length) {