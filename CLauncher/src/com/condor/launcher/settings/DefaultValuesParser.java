package com.condor.launcher.settings;

import android.content.Context;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.Xml;

import com.android.launcher3.R;
import com.condor.launcher.util.Constants;
import com.condor.launcher.util.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Perry on 19-1-11
 */
public class DefaultValuesParser {
    private static final String TAG = "DefaultValuesParser";

    private static final SparseBooleanArray ALL_BOOLEAN_ARRAY = new SparseBooleanArray();
    private static final SparseIntArray ALL_INT_ARRAY = new SparseIntArray();
    private static final SparseArray<String> ALL_STRING_ARRAY = new SparseArray<String>();

    public void saveBooleanValue(DefaultKey key, SettingsPersistence.BooleanPersistence persistence) {
        persistence.save(getDefaultBooleanValue(key, persistence.defaultValue()));
    }

    public void saveStringValue(DefaultKey key, SettingsPersistence.StringPersistence persistence) {
        persistence.save(getDefaultStringValue(key, persistence.defaultValue()));
    }

    public void saveIntegerValue(DefaultKey key, SettingsPersistence.IntegerPersistence persistence) {
        persistence.save(getDefaultIntegerValue(key, persistence.defaultValue()));
    }
    
    private boolean getDefaultBooleanValue(DefaultKey key, boolean defaultValue) {
        if (ALL_BOOLEAN_ARRAY.indexOfKey(key.intKey()) < 0) {
            return defaultValue;
        }

        boolean result = ALL_BOOLEAN_ARRAY.get(key.intKey());
        Logger.d(TAG, "bool " + key.stringKey() + "("+key.intKey()+")" + ":" + result
                + "->[" + defaultValue + "]");
        return result;
    }

    private String getDefaultStringValue(DefaultKey key, String defaultValue) {
        String result = ALL_STRING_ARRAY.get(key.intKey());
        Logger.d(TAG, "string " + key.stringKey() + "("+key.intKey()+")" + ":" + result
                + "->[" + defaultValue + "]");
        if (result == null) {
            return defaultValue;
        }

        return result;
    }

    private int getDefaultIntegerValue(DefaultKey key, int defaultValue) {
        if (ALL_INT_ARRAY.indexOfKey(key.intKey()) < 0) {
            return defaultValue;
        }
        int result = ALL_INT_ARRAY.get(key.intKey());
        Logger.d(TAG, "integer " + key.stringKey() + "("+key.intKey()+")" + ":" + result
                + "->[" + defaultValue + "]");
        return result;
    }

    public DefaultValuesParser(Context context) {
        File file = new File(Constants.CONFIG_FILE_NAME_PATH);

        if (file.exists()) {
            InputStream in = null;

            try {
                in = new FileInputStream(file);
                doParse(in);
            } catch (IOException e) {
                Logger.d(TAG, "Read default values file error", e);
                doParse(getDefaultFileInputStream(context));
            }
        } else {
            Logger.d(TAG, "The default values file does not exist!");
            doParse(getDefaultFileInputStream(context));
        }
    }

    private InputStream getDefaultFileInputStream(Context context) {
        return context.getResources().openRawResource(R.raw.setting_default_values);
    }

    private void doParse(InputStream is) {
        XmlPullParser parser = Xml.newPullParser();

        try {
            parser.setInput(is, "utf-8");

            startTraversal(parser);

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startTraversal(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    Logger.i(TAG, "start parsing");
                    break;

                case XmlPullParser.START_TAG:
                    parseTag(parser);
                    break;

                case XmlPullParser.END_TAG:
                    Logger.i(TAG, "end a tag");
                    break;
            }
            eventType = parser.next();
        }

    }

    private static final class FileTAG {
        private static final String TAG_BOOL = "bool";
        private static final String TAG_INT  = "int";
        private static final String TAG_STRING = "string";
        private static final String ATTRIBUTE_NAME = "name";
    }

    private void parseTag(XmlPullParser parser) {
        String tagName = parser.getName();
        Logger.i(TAG, "tagName = " + tagName);
        parseValues(tagName, parser);
    }

    private void parseValues(String tagName, XmlPullParser parser) {
        for (int i = 0, count = parser.getAttributeCount(); i < count; i++) {
            String name = parser.getAttributeName(i);
            if (FileTAG.ATTRIBUTE_NAME.equals(name)) {
                String value = parser.getAttributeValue(i);
                String text;
                try {
                    text = parser.nextText();
                    int key = DefaultKey.getIntKey(value);
                    if (FileTAG.TAG_BOOL.equals(tagName)) {
                        ALL_BOOLEAN_ARRAY.put(key, Boolean.parseBoolean(text));
                    } else if (FileTAG.TAG_INT.equals(tagName)) {
                        ALL_INT_ARRAY.put(key, Integer.parseInt(text));
                    } else if (FileTAG.TAG_STRING.equals(tagName)) {
                        ALL_STRING_ARRAY.put(key, text);
                    }
                    Logger.i(TAG, "ATTRIBUTE_NAME " + name + ",value is " + value
                            + ",text is " + text + ", key is " + key);
                } catch (Exception e) {
                    Logger.e(TAG, "get text error for " + name);
                }
            }

        }
    }
}
