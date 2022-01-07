package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class UnionType extends Type {

    public Set<Type> types;


    public UnionType() {
        th