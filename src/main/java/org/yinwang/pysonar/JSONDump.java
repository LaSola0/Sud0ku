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
import j