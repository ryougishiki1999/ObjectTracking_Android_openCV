package com.nju.cs.zrh.objecttracking.utils;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MyDateUtils {

    public static long getCurTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * @param
     * @return milliseconds(long)
     * @description
     * @author
     * @time
     */
    public static long getTimeDistance(long timestamp1, long timestamp2) {

        if (timestamp1 > timestamp2) {
            return getTimeDistance(timestamp2, timestamp1);
        }

        long diff = timestamp2 - timestamp1;
        return diff;
    }
}
