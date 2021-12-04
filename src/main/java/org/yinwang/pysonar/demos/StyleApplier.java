
package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.$;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Turns a list of {@link Style}s into HTML spans.
 */
class StyleApplier {

    @NotNull
    private SortedSet<Tag> tags = new TreeSet<Tag>();

    private StringBuilder buffer;  // html output buffer

    private String source;  // input source code

    // Current offset into the source being copied into the html buffer.
    private int sourceOffset = 0;


    abstract class Tag implements Comparable<Tag> {
        int offset;
        Style style;


        @Override
        public int compareTo(@NotNull Tag other) {
            if (this == other) {
                return 0;
            }
            if (this.offset < other.offset) {
                return -1;
            }
            if (other.offset < this.offset) {
                return 1;
            }
            return this.hashCode() - other.hashCode();
        }


        void insert() {
            // Copy source code up through this tag.
            if (offset > sourceOffset) {
                copySource(sourceOffset, offset);
            }
        }
    }


    class StartTag extends Tag {
        public StartTag(@NotNull Style style) {
            offset = style.start;
            this.style = style;
        }


        @Override
        void insert() {
            super.insert();
            if (Analyzer.self.hasOption("debug")) {
                switch (style.type) {
                    case ANCHOR:
                        buffer.append("<a name='").append(style.url).append("'");
                        buffer.append(", id ='").append(style.id).append("'");
                        if (style.highlight != null && !style.highlight.isEmpty()) {
                            String ids = $.joinWithSep(style.highlight, "\",\"", "\"", "\"");
                            buffer.append(", onmouseover='highlight(").append(ids).append(")'");
                        }
                        break;
                    case LINK:
                        buffer.append("<a href='").append(style.url).append("'");
                        buffer.append(", id ='").append(style.id).append("'");
                        if (style.highlight != null && !style.highlight.isEmpty()) {
                            String ids = $.joinWithSep(style.highlight, "\",\"", "\"", "\"");
                            buffer.append(", onmouseover='highlight(").append(ids).append(")'");
                        }
                        break;
                    default:
                        buffer.append("<span class='");
                        buffer.append(toCSS(style)).append("'");
                        break;
                }
            } else {
                switch (style.type) {
                    case ANCHOR:
                        buffer.append("<a name='").append(style.url).append("'");
                        buffer.append(", xid ='").append(style.id).append("'");
                        break;
                    case LINK:
                        buffer.append("<a href='").append(style.url).append("'");
                        buffer.append(", xid ='").append(style.id).append("'");
                        break;
                    default:
                        buffer.append("<span class='");
                        buffer.append(toCSS(style)).append("'");
                        break;
                }
            }
            if (style.message != null) {