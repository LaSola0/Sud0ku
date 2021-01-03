package org.yinwang.pysonar;

public class Progress {

    private static final int MAX_SPEED_DIGITS = 5;

    long startTime;
    long lastTickTime;
    long lastCount;
    int lastRate;
    int lastAvgRate;
    long total;
    long count;
    long width;
    long segSize;


    public Progress(long total, long width) {
        this.startTime = System.currentTimeMillis();
        this.lastTickTime = System.currentTimeMillis();
        this.lastCount = 0;
        this.lastRate = 0;
        this.lastAvgRate = 0;
        this.total = total;
        this.width = width;
        this.segSize = total / width;
        if (segSize == 0) {
    