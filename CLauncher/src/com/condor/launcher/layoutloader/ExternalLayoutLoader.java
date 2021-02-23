package com.condor.launcher.layoutloader;

import android.appwidget.AppWidgetHost;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.android.launcher3.LauncherSettings;
import com.android.launcher3.util.Thunk;
import com.condor.launcher.util.Constants;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Perry on 19-1-14.
 */
public class ExternalLayoutLoader extends BaseLayoutLoader {
    protected static final String TAG_RESOLVE = "resolve";
    private static final String TAG_FAVORITES = "favorites";

    private static final String ATTR_FOLDER_ITEMS = "folderItems";

    private final String mFile;

    public ExternalLayoutLoader(Context context, AppWidgetHost appWidgetHost,
                                LayoutParserCallback callback, Resources sourceRes, String file) {
        super(context, appWidgetHost, callback, sourceRes, TAG_FAVORITES);
        mFile = file;
    }

    @Override
    protected int parseLayout(ArrayList<Long> screenIds) throws XmlPullParserException, IOException {
        return parseLayout(mFile, screenIds);
    }

    @Override
    protected int parseInclude(XmlPullParser parser, ArrayList<Long> screenIds) throws XmlPullParserException, IOException {
        if (TAG_INCLUDE.equals(parser.getName())) {
            final String file = getAttributeValue(parser, ATTR_WORKSPACE);
            if (!TextUtils.isEmpty(file)) {
                // recursively load some more favorites, why not?
                return parseLayout(file, screenIds);
            } else {
                return 0;
            }
        }
        return -1;
    }

    protected int parseLayout(String file, ArrayList<Long> screenIds) throws IOException, XmlPullParserException {
        String path = Constants.LAUNCHER_CONFIG_DIRECTION + file;
        Log.d("ExternalLayoutLoader","defaultLayout="+path);
        if (!new File(path).exists()) return 0;

        XmlPullParser parser = Xml.newPullParser();
        int count = 0;
        FileInputStream is = new FileInputStream(path);
        try {
            parser.setInput(is, "utf-8");
            beginDocument(parser, mRootTag);
            final int depth = parser.getDepth();
            int type;
            HashMap<String, TagParser> tagParserMap = getLayoutElementsMap();

            while (((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                count += parseAndAddNode(parser, tagParserMap, screenIds);
            }
        } finally {
            is.close();
        }
        return count;
    }

    @Override
    protected HashMap<String, TagParser> getFolderElementsMap() {
        return getFolderElementsMap(mSourceRes);
    }

    @Thunk
    HashMap<String, TagParser> getFolderElementsMap(Resources res) {
        HashMap<String, TagParser> parsers = new HashMap<String, TagParser>();
        parsers.put(TAG_FAVORITE, new AppShortcutWithUriParser());
        parsers.put(TAG_SHORTCUT, new UriShortcutParser(res));
        return parsers;
    }

    @Override
    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<String, TagParser>();
        parsers.put(TAG_FAVORITE, new AppShortcutWithUriParser());
        parsers.put(TAG_APPWIDGET, new AppWidgetParser());
        parsers.put(TAG_SHORTCUT, new UriShortcutParser(mSourceRes));
        parsers.put(TAG_RESOLVE, new ResolveParser());
        parsers.put(TAG_FOLDER, new MyFolderParser());
        return parsers;
    }

    @Override
    protected void parseContainerAndScreen(XmlPullParser parser, long[] out) {
        out[0] = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        String strContainer = getAttributeValue(parser, ATTR_CONTAINER);
        if (strContainer != null) {
            out[0] = Long.valueOf(strContainer);
        }
        out[1] = Long.parseLong(getAttributeValue(parser, ATTR_SCREEN));
    }

    /**
     * An extension of FolderParser which allows adding items from a different xml.
     */
    @Thunk
    class MyFolderParser extends FolderParser {

        @Override
        public long parseAndAdd(XmlPullParser parser) throws XmlPullParserException,
                IOException {
            final String file = getAttributeValue(parser, ATTR_FOLDER_ITEMS);
            if (!TextUtils.isEmpty(file)) {
                String path = Constants.LAUNCHER_CONFIG_DIRECTION + file;
                if (!new File(path).exists()) return super.parseAndAdd(parser);

                parser = Xml.newPullParser();
                parser.setInput(new FileInputStream(path), "utf-8");
                beginDocument(parser, TAG_FOLDER);
            }
            return super.parseAndAdd(parser);
        }
    }

    /**
     * Return attribute value, attempting launcher-specific namespace first
     * before falling back to anonymous attribute.
     */
    @Override
    protected String getAttributeValue(XmlPullParser parser, String attribute) {
        return parser.getAttributeValue(null, attribute);
    }
}
