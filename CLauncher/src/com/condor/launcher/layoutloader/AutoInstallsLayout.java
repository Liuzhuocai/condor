/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.condor.launcher.layoutloader;

import android.appwidget.AppWidgetHost;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;
import android.util.Pair;


import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Perry on 19-1-14.
 *
 * Layout parsing code for auto installs layout
 */
public class AutoInstallsLayout extends BaseLayoutLoader {
    protected static final String TAG = "AutoInstalls";

    /** Marker action used to discover a package which defines launcher customization */
    static final String ACTION_LAUNCHER_CUSTOMIZATION =
            "android.autoinstalls.config.action.PLAY_AUTO_INSTALL";

    /**
     * Layout resource which also includes grid size and hotseat count, e.g., default_layout_6x6_h5
     */
    private static final String FORMATTED_LAYOUT_RES_WITH_HOSTEAT = "default_layout_%dx%d_h%s";
    private static final String FORMATTED_LAYOUT_RES = "default_layout_%dx%d";
    private static final String LAYOUT_RES = "default_layout";

    public static AutoInstallsLayout get(Context context, AppWidgetHost appWidgetHost,
                                         LayoutParserCallback callback) {
        Pair<String, Resources> customizationApkInfo = Utilities.findSystemApk(
                ACTION_LAUNCHER_CUSTOMIZATION, context.getPackageManager());
        if (customizationApkInfo == null) {
            return null;
        }
        return get(context, customizationApkInfo.first, customizationApkInfo.second,
                appWidgetHost, callback);
    }

    public static AutoInstallsLayout get(Context context, String pkg, Resources targetRes,
                                         AppWidgetHost appWidgetHost, LayoutParserCallback callback) {
        InvariantDeviceProfile grid = LauncherAppState.getIDP(context);

        // Try with grid size and hotseat count
        String layoutName = String.format(Locale.ENGLISH, FORMATTED_LAYOUT_RES_WITH_HOSTEAT,
                (int) grid.numColumns, (int) grid.numRows, (int) grid.numHotseatIcons);
        int layoutId = targetRes.getIdentifier(layoutName, "xml", pkg);

        // Try with only grid size
        if (layoutId == 0) {
            Log.d(TAG, "Formatted layout: " + layoutName
                    + " not found. Trying layout without hosteat");
            layoutName = String.format(Locale.ENGLISH, FORMATTED_LAYOUT_RES,
                    (int) grid.numColumns, (int) grid.numRows);
            layoutId = targetRes.getIdentifier(layoutName, "xml", pkg);
        }

        // Try the default layout
        if (layoutId == 0) {
            Log.d(TAG, "Formatted layout: " + layoutName + " not found. Trying the default layout");
            layoutId = targetRes.getIdentifier(LAYOUT_RES, "xml", pkg);
        }

        if (layoutId == 0) {
            Log.e(TAG, "Layout definition not found in package: " + pkg);
            return null;
        }
        return new AutoInstallsLayout(context, appWidgetHost, callback, targetRes, layoutId,
                TAG_WORKSPACE);
    }

    private static final String ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE =
            "com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";

    protected final int mLayoutId;

    public AutoInstallsLayout(Context context, AppWidgetHost appWidgetHost,
                              LayoutParserCallback callback, Resources res,
                              int layoutId, String rootTag) {
        super(context, appWidgetHost, callback, res, rootTag);
        mLayoutId = layoutId;
    }

    /**
     * Parses the layout and returns the number of elements added on the homescreen.
     */
    @Override
    protected int parseLayout(ArrayList<Long> screenIds) throws IOException, XmlPullParserException {
        return parseLayout(mLayoutId, screenIds);
    }

    @Override
    protected int parseInclude(final XmlPullParser parser, ArrayList<Long> screenIds) throws XmlPullParserException, IOException {
        if (TAG_INCLUDE.equals(parser.getName())) {
            final int resId = getAttributeResourceValue(parser, ATTR_WORKSPACE, 0);
            if (resId != 0) {
                // recursively load some more favorites, why not?
                return parseLayout(resId, screenIds);
            } else {
                return 0;
            }
        }
        return -1;
    }

    protected int parseLayout(int layoutId, ArrayList<Long> screenIds) throws IOException, XmlPullParserException {
        XmlResourceParser parser = mSourceRes.getXml(layoutId);
        beginDocument(parser, mRootTag);
        final int depth = parser.getDepth();
        int type;
        HashMap<String, TagParser> tagParserMap = getLayoutElementsMap();
        int count = 0;

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            count += parseAndAddNode(parser, tagParserMap, screenIds);
        }
        return count;
    }

    /**
     * Return attribute value, attempting launcher-specific namespace first
     * before falling back to anonymous attribute.
     */
    @Override
    protected String getAttributeValue(XmlPullParser parser, String attribute) {
        String value = parser.getAttributeValue(
                "http://schemas.android.com/apk/res-auto", attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    /**
     * Return attribute resource value, attempting launcher-specific namespace
     * first before falling back to anonymous attribute.
     */
    @Override
    protected int getAttributeResourceValue(XmlPullParser parser, String attribute,
                                            int defaultValue) {
        if (parser instanceof XmlResourceParser) {
            int value = ((XmlResourceParser)parser).getAttributeResourceValue(
                    "http://schemas.android.com/apk/res-auto", attribute,
                    defaultValue);
            if (value == defaultValue) {
                value = ((XmlResourceParser)parser).getAttributeResourceValue(null, attribute, defaultValue);
            }
            return value;
        }

        return super.getAttributeResourceValue(parser, attribute, defaultValue);
    }
}
