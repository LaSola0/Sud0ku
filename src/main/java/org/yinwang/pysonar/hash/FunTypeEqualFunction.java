package org.yinwang.pysonar.hash;

import org.yinwang.pysonar.types.FunType;


public class FunTypeEqualFunction extends EqualFunction {

    @Override
    public boolean equals(Object x, Object y) {
        if (x instanceof FunType && y instanceof FunType) {
            FunType 