package org.yinwang.pysonar;

import java.util.ArrayList;
import java.util.List;

public class Stack<T>
{
    private List<T> content = new ArrayList<>();

    public void push(T item)
    {
        content.add(item);
   