package com.sing.legacycompletion.utils;

import java.util.Arrays;

public class Utils {
    public static int getLastArgumentStart(int pos, String text) {
        final int i = text.lastIndexOf(' ', pos);
        return i+1;
    }
    public static <T> T[] dropFirst(T[] array){
        return Arrays.copyOfRange(array,1,array.length);
    }
}
