
package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.types.ClassType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * Generates a file outline from the index: a structure representing the
 * variable and attribute definitions in a file.
 */
public class Outliner {

    public static abstract class Entry {
        @Nullable
        public String qname;  // entry qualified name
        public int offset;  // file offset of referenced declaration
        @Nullable
        public Binding.Kind kind;  // binding kind of outline entry


        public Entry() {
        }


        public Entry(String qname, int offset, Binding.Kind kind) {
            this.qname = qname;
            this.offset = offset;
            this.kind = kind;
        }


        public abstract boolean isLeaf();


        @NotNull
        public Leaf asLeaf() {
            return (Leaf) this;
        }


        public abstract boolean isBranch();


        @NotNull
        public Branch asBranch() {
            return (Branch) this;
        }


        public abstract boolean hasChildren();


        public abstract List<Entry> getChildren();


        public abstract void setChildren(List<Entry> children);


        @Nullable
        public String getQname() {
            return qname;
        }


        public void setQname(@Nullable String qname) {
            if (qname == null) {
                throw new IllegalArgumentException("qname param cannot be null");
            }
            this.qname = qname;
        }

