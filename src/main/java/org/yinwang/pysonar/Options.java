package org.yinwang.pysonar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Options {

    private Map<String, Object> optionsMap = new LinkedHashMap<>();


    private List<String> args = new ArrayList<>();


    public Options(String[] args) {
        for (int i = 0; i < args.length