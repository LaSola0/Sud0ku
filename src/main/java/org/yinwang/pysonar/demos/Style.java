package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.$;

import java.util.List;


/**
 * Represents a simple style run for purposes of source highlighting.
 */
public class Style implements Comparable<Style> {

    public enum Type {
        KEYWORD,
        COMMENT,
        STRING,
        DOC_STRING,
        IDENTIFIER,
        BUILTIN,
        NUMBER,
        CONSTANT,       // ALL_CAPS identifier
        FUNCTION,       // function name
        PARAMETER,      // function parameter
        LOCAL,          // local variable
        DECORATOR,      // function decorator
        CLASS,          // class name
   