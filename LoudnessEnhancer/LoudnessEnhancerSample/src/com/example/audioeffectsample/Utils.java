package com.example.audioeffectsample;

public class Utils {

    public static int parseInt(String text) {
        try {
            if (text!=null && text.length() > 0) {
                return Integer.parseInt(text);
            }
        } catch (NumberFormatException e) {
        }
        return 0;
    }
    
    public static float parseFloat(String text) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (('0' <= c && c <= '9') || c == '.' || c == '-') {
                buf.append(c);
            } else {
                break;
            }
        }
        if (buf.length() > 0) {
            return Float.parseFloat(buf.toString());
        }
        return 0;
    }

}
