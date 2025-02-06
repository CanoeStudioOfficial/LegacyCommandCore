package com.sing.legacycompletion.utils;

public class Utils {
    public static int getLastArgumentStart(int pos, String text) {
        final int i = text.lastIndexOf(' ', pos);
        return i+1;
    }
}
