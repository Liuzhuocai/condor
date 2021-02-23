package com.condor.launcher.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import java.util.ArrayList;

/**
 * Created by Perry on 19-1-11
 *
 * Reference the library of
 * <a href="https://github.com/xeoh/TextHighlighter">TextHighlighter</a>
 */
public class TextHighlighter {
    private ForegroundColorSpan fgColor;
    private BackgroundColorSpan bgColor;
    private UnderlineSpan underline;
    private boolean bold = false;
    private boolean italic = false;
    private String highlightedText = null;
    private Matcher matcher;

    /**
     * Its method will be called to find position of highlighting text.
     */
    public interface Matcher {
        /**
         * Implement algorithm to find matching indices.
         *
         * <p> getMatchIndices("Hello world! We are the world". "world") should return [7,14]
         *
         * @param origin  whole text
         * @param keyword word you want to highlight
         * @return matching indies
         */
        ArrayList<Integer> getMatchIndices(String origin, String keyword);

        /**
         * Implement algorithm to detecting any matching word exist.
         *
         * @param origin  whole text
         * @param keyword word you want to highlight
         * @return true for if any text matches, otherwise false
         */
        boolean isHighlightable(String origin, String keyword);
    }

    /**
     * Simple Matcher which finds exactly same word.
     */
    public static final Matcher BASE_MATCHER = new Matcher() {
        @Override
        public ArrayList<Integer> getMatchIndices(String origin, String keyword) {
            ArrayList<Integer> indices = new ArrayList<>();

            int index = origin.indexOf(keyword, 0);
            while (index != -1) {
                indices.add(index);
                index = origin.indexOf(keyword, index + 1);
            }

            return indices;
        }

        @Override
        public boolean isHighlightable(String origin, String keyword) {
            return getMatchIndices(origin, keyword).size() > 0;
        }
    };

    /**
     * Case insensitive Matcher which finds exactly same word but ignore case.
     */
    public static final Matcher CASE_INSENSITIVE_MATCHER = new Matcher() {
        @Override
        public ArrayList<Integer> getMatchIndices(String origin, String keyword) {
            ArrayList<Integer> indices = new ArrayList<>();

            origin = origin.toLowerCase();
            keyword = keyword.toLowerCase();

            int index = origin.indexOf(keyword, 0);
            while (index != -1) {
                indices.add(index);
                index = origin.indexOf(keyword, index + 1);
            }

            return indices;
        }

        @Override
        public boolean isHighlightable(String origin, String keyword) {
            return getMatchIndices(origin, keyword).size() > 0;
        }
    };

    /**
     * Set foreground color of highlighted text.
     *
     * @param color color of foreground.
     * @return itself
     */
    public TextHighlighter setForegroundColor(int color) {
        fgColor = new ForegroundColorSpan(color);
        return this;
    }

    /**
     * Reset foreground color of highlighted text.
     *
     * @return itself
     */
    public TextHighlighter resetForegroundColor() {
        fgColor = null;
        return this;
    }

    /**
     * Set background color of highlighted text.
     *
     * @param color color of background
     * @return itself
     */
    public TextHighlighter setBackgroundColor(int color) {
        bgColor = new BackgroundColorSpan(color);
        return this;
    }

    /**
     * Reset background color of highlighted text.
     *
     * @return itself
     */
    public TextHighlighter resetBackgroundColor() {
        bgColor = null;
        return this;
    }

    /**
     * Set underline of highlighted text.
     *
     * @param underline underline or not
     * @return itself
     */
    public TextHighlighter setUnderline(boolean underline) {
        this.underline = underline ? new UnderlineSpan() : null;
        return this;
    }

    /**
     * Set bold typeface to highlighted text.
     *
     * @param bold bold or not
     * @return itself
     */
    public TextHighlighter setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    /**
     * Set italic typeface to highlighted text.
     *
     * @param italic bold or not
     * @return itself
     */
    public TextHighlighter setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    /**
     * Set matcher to highlighted text.
     *
     * @param matcher matcher
     * @return itself
     */
    public TextHighlighter setMatcher(Matcher matcher) {
        this.matcher = matcher;
        return this;
    }

    /**
     * Set keyword to highlighted text.
     *
     * @param highlightedText keyword
     * @return itself
     */
    public TextHighlighter setHighlightedText(String highlightedText) {
        this.highlightedText = highlightedText;
        return this;
    }

    public Spannable getHighlightedText(String text) {
        if (matcher == null) {
            matcher = BASE_MATCHER;
        }
        return getHighlightedText(text, highlightedText,
                matcher.getMatchIndices(text, highlightedText));
    }

    public Spannable getHighlightedText(Spannable origin, String text, int start) {
        if (matcher == null) {
            matcher = BASE_MATCHER;
        }
        return getHighlightedText(origin, highlightedText,
                matcher.getMatchIndices(text, highlightedText), start);
    }

    public Spannable getHighlightedText(String origin, String keyword,
                                         ArrayList<Integer> indices) {
        return getHighlightedText(new SpannableString(origin), keyword, indices, 0);
    }

    public Spannable getHighlightedText(Spannable origin, String keyword,
                                         ArrayList<Integer> indices, int start) {
        Spannable spannable = new SpannableString(origin.toString());

        boolean noop = origin.toString().isEmpty()
                || (fgColor == null && bgColor == null && !bold && !italic);

        if (noop) {
            return spannable;
        }

        for (int index = 0; index < origin.toString().length(); index++) {
            for (CharacterStyle style : origin.getSpans(index, index + 1, CharacterStyle.class)) {
                if (!(style instanceof StyleSpan)) {
                    spannable.setSpan(CharacterStyle.wrap(style), index, index + 1, 0);
                }
            }
        }

        for (int index : indices) {
            if (index < start) {
                continue;
            }
            if (fgColor != null) {
                spannable.setSpan(CharacterStyle.wrap(fgColor), index, index + keyword.length(), 0);
            }

            if (bgColor != null) {
                spannable.setSpan(CharacterStyle.wrap(bgColor), index, index + keyword.length(), 0);
            }

            if (underline != null) {
                spannable.setSpan(CharacterStyle.wrap(underline), index, index + keyword.length(), 0);
            }

            if (!bold && !italic) {
                spannable.setSpan(new StyleSpan(Typeface.NORMAL), index, index + keyword.length(), 0);
            } else if (bold && !italic) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), index, index + keyword.length(), 0);
            } else if (!bold && italic) {
                spannable.setSpan(new StyleSpan(Typeface.ITALIC), index, index + keyword.length(), 0);
            } else {
                spannable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), index, index + keyword.length(), 0);
            }
        }

        return spannable;
    }
}