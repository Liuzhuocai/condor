package com.condor.launcher.search;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Perry on 19-1-15
 */
public class FuzzyMatcher {
    private static final String TAG = "FuzzyMatcher";
    private static final HashMap<String, String> ESCAPES = new HashMap<>();

    static {
        initEscapes();
    }

    public static void initEscapes() {
        ESCAPES.put(".", "\\.");
        ESCAPES.put("*", "\\*");
        ESCAPES.put("^", "\\^");
        ESCAPES.put("$", "\\$");
        ESCAPES.put("]", "\\]");
        ESCAPES.put("[", "\\[");
        ESCAPES.put("(", "\\(");
        ESCAPES.put(")", "\\)");
        ESCAPES.put("}", "\\}");
        ESCAPES.put("{", "\\{");
        ESCAPES.put("|", "\\|");
        ESCAPES.put("\\", "\\\\");
        ESCAPES.put("+", "\\+");
    }

    public static Pattern toFuzzyKey(String key, KeyInfo outKeyInfo) {
        HashMap<String, String> escapes = ESCAPES;
        StringBuilder sb = new StringBuilder();
        sb.append(".*?(");
        List<Token> tokens = getTokens(key);
        for (int j = 0; j < tokens.size(); j++) {
            Token token = tokens.get(j);
            if (token.type == Token.PINYIN) {
                if (j == 0) {
                    sb.append(token.target + ").*?");
                } else {
                    sb.append(token.target + ".*?");
                }
                outKeyInfo.hasHanZi = true;
            } else {
                String target = token.target;
                int len = target.length();
                for (int i = 0; i < len; i++) {
                    String c = Character.toString(target.charAt(i));
                    String conv = escapes.get(c);
                    if (conv == null) {
                        conv = c;
                    }
                    if (i == 0 && j == 0) {
                        sb.append(conv + ").*?");
                    } else {
                        sb.append(conv + ".*?");
                    }
                }
            }
        }

        String fuzzyKey = sb.toString();
        try {
            Pattern patt = Pattern.compile(fuzzyKey, Pattern.CASE_INSENSITIVE);
            return patt;
        } catch (PatternSyntaxException ex) {
            return null;
        }
    }

    public static List<Token> getTokens(String inputString) {
        List<Token> tokens = new ArrayList<>();
        if (TextUtils.isEmpty(inputString)) {
            return tokens;
        }

        StringBuilder latin = new StringBuilder();
        char[] input = inputString.trim().toCharArray();
        for (int i = 0; i < input.length; i++) {
            latin.append(Character.toString(input[i]));
        }

        if (latin.length() > 0) {
            tokens.add(new Token(Token.LATIN, latin.toString()));
        }
        return tokens;
    }

    public static String toLatin(String inputString) {
        if (TextUtils.isEmpty(inputString)) {
            return "";
        }

        StringBuilder latin = new StringBuilder();
        char[] input = inputString.trim().toCharArray();
        for (int i = 0; i < input.length; i++) {
            latin.append(Character.toString(input[i]));
        }
        return latin.toString();
    }

    private static class Token {
        public static final int LATIN = 1;
        public static final int PINYIN = 2;
        public static final int UNKNOWN = 3;

        public int type;
        public String target;

        public Token() {
        }

        public Token(int type, String target) {
            this.type = type;
            this.target = target;
        }
    }

    public static class KeyInfo {
        public String originKey;
        public boolean hasHanZi;
        public Pattern fuzzyKey;
        public boolean isBlank;
    }

    public static class MatcherInfo {
        public String title;
        public String spellName;
    }

    public static KeyInfo getKeyInfo(String key) {
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.originKey = key;
        if (key != null) {
            String keyStrip = key.replaceAll("\\s*|[\n\r]*", "");
            if (keyStrip.equals("")) {
                keyInfo.isBlank = true;
            } else {
                keyInfo.fuzzyKey = toFuzzyKey(key, keyInfo);
            }
        }
        return keyInfo;
    }

    public static boolean matches(KeyInfo key, String input) {
        if (key.isBlank || key.fuzzyKey == null) {
            return false;
        }
        Pattern fuzzyKey = key.fuzzyKey;
        Matcher m = fuzzyKey.matcher(input);

        return m.matches();
    }

    public static boolean matches(KeyInfo key, MatcherInfo input) {
        if (key.isBlank || key.fuzzyKey == null || input.title == null) {
            return false;
        }
        return matches(key, input.spellName);
    }

    public static int indexOf(KeyInfo key, MatcherInfo input) {
        if (key.isBlank || key.fuzzyKey == null || input.title == null) {
            return -1;
        }
        Pattern fuzzyKey = key.fuzzyKey;
        Matcher m = fuzzyKey.matcher(input.spellName);

        if (m.matches()) {
            return m.start(1);
        }

        return -1;
    }

    public static void clear() {
        ESCAPES.clear();
    }
}
