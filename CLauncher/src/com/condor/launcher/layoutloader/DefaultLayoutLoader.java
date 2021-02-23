package com.condor.launcher.layoutloader;

import android.appwidget.AppWidgetHost;
import android.content.Context;
import android.content.res.Resources;


import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Partner;
import com.android.launcher3.util.Thunk;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Perry on 19-1-14.
 *
 * Implements the layout parser with rules for internal layouts and partner layouts.
 */
public class DefaultLayoutLoader extends AutoInstallsLayout {
    private static final String TAG = "DefaultLayoutLoader";

    protected static final String TAG_RESOLVE = "resolve";
    private static final String TAG_FAVORITES = "favorites";
    private static final String TAG_PARTNER_FOLDER = "partner-folder";

    private static final String ATTR_FOLDER_ITEMS = "folderItems";

    // TODO: Remove support for this broadcast, instead use widget options to send bind time options

    public DefaultLayoutLoader(Context context, AppWidgetHost appWidgetHost,
                               LayoutParserCallback callback, Resources sourceRes, int layoutId) {
        super(context, appWidgetHost, callback, sourceRes, layoutId, TAG_FAVORITES);
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
        parsers.put(TAG_PARTNER_FOLDER, new PartnerFolderParser());
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
     * A parser which adds a folder whose contents come from partner apk.
     */
    @Thunk
    class PartnerFolderParser implements TagParser {

        @Override
        public long parseAndAdd(XmlPullParser parser) throws XmlPullParserException,
                IOException {
            // Folder contents come from an external XML resource
            final Partner partner = Partner.get(mPackageManager);
            if (partner != null) {
                final Resources partnerRes = partner.getResources();
                final int resId = partnerRes.getIdentifier(Partner.RES_FOLDER,
                        "xml", partner.getPackageName());
                if (resId != 0) {
                    final XmlPullParser partnerParser = partnerRes.getXml(resId);
                    beginDocument(partnerParser, TAG_FOLDER);

                    FolderParser folderParser = new FolderParser(getFolderElementsMap(partnerRes));
                    return folderParser.parseAndAdd(partnerParser);
                }
            }
            return -1;
        }
    }

    /**
     * An extension of FolderParser which allows adding items from a different xml.
     */
    @Thunk
    class MyFolderParser extends FolderParser {

        @Override
        public long parseAndAdd(XmlPullParser parser) throws XmlPullParserException,
                IOException {
            final int resId = getAttributeResourceValue(parser, ATTR_FOLDER_ITEMS, 0);
            if (resId != 0) {
                parser = mSourceRes.getXml(resId);
                beginDocument(parser, TAG_FOLDER);
            }
            return super.parseAndAdd(parser);
        }
    }
}
