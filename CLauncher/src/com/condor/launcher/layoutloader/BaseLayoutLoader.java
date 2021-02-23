package com.condor.launcher.layoutloader;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;


import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.util.Thunk;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Perry on 19-1-14.
 */
public abstract class BaseLayoutLoader {
    protected static final String TAG = "BaseLayoutLoader";
    protected static final boolean LOGD = false;

    // Object Tags
    protected static final String TAG_FAVORITE = "favorite";

    protected static final String TAG_INCLUDE = "include";
    protected static final String TAG_WORKSPACE = "workspace";
    protected static final String TAG_APP_ICON = "appicon";
    protected static final String TAG_AUTO_INSTALL = "autoinstall";
    protected static final String TAG_FOLDER = "folder";
    protected static final String TAG_APPWIDGET = "appwidget";
    protected static final String TAG_SHORTCUT = "shortcut";
    protected static final String TAG_EXTRA = "extra";

    protected static final String ATTR_CONTAINER = "container";
    protected static final String ATTR_RANK = "rank";

    protected static final String ATTR_PACKAGE_NAME = "packageName";
    protected static final String ATTR_CLASS_NAME = "className";
    protected static final String ATTR_TITLE = "title";
    protected static final String ATTR_SCREEN = "screen";

    // x and y can be specified as negative integers, in which case -1 represents the
    // last row / column, -2 represents the second last, and so on.
    protected static final String ATTR_X = "x";
    protected static final String ATTR_Y = "y";

    protected static final String ATTR_SPAN_X = "spanX";
    protected static final String ATTR_SPAN_Y = "spanY";
    protected static final String ATTR_ICON = "icon";
    protected static final String ATTR_URL = "url";
    protected static final String ATTR_URI = "uri";

    // Attrs for "Include"
    protected static final String ATTR_WORKSPACE = "workspace";

    // Style attrs -- "Extra"
    protected static final String ATTR_KEY = "key";
    protected static final String ATTR_VALUE = "value";

    protected static final String HOTSEAT_CONTAINER_NAME =
            LauncherSettings.Favorites.containerToString(LauncherSettings.Favorites.CONTAINER_HOTSEAT);

    private static final String ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE =
            "com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";

    protected final Context mContext;
    protected final AppWidgetHost mAppWidgetHost;
    protected final LayoutParserCallback mCallback;

    private final long[] mTemp = new long[2];
    protected final PackageManager mPackageManager;
    protected final Resources mSourceRes;

    protected final InvariantDeviceProfile mIdp;
    protected final int mRowCount;
    protected final int mColumnCount;

    protected final ContentValues mValues;
    protected final String mRootTag;

    protected SQLiteDatabase mDb;

    public BaseLayoutLoader(Context context, AppWidgetHost appWidgetHost,
                            LayoutParserCallback callback, Resources res, String rootTag) {
        mContext = context;
        mAppWidgetHost = appWidgetHost;
        mCallback = callback;

        mPackageManager = context.getPackageManager();
        mSourceRes = res;

        mValues = new ContentValues();
        mRootTag = rootTag;

        mIdp = LauncherAppState.getIDP(context);
        mRowCount = mIdp.numRows;
        mColumnCount = mIdp.numColumns;
    }

    /**
     * Loads the layout in the db and returns the number of entries added on the desktop.
     */
    public int loadLayout(SQLiteDatabase db, ArrayList<Long> screenIds) {
        mDb = db;
        try {
            return parseLayout(screenIds);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing layout: ", e);
            return -1;
        }
    }

    protected abstract int parseLayout(ArrayList<Long> screenIds) throws XmlPullParserException, IOException;

    protected abstract int parseInclude(final XmlPullParser parser, ArrayList<Long> screenIds) throws XmlPullParserException, IOException;

    /**
     * Parses container and screenId attribute from the current tag, and puts it in the out.
     * @param out array of size 2.
     */
    protected void parseContainerAndScreen(XmlPullParser parser, long[] out) {
        if (HOTSEAT_CONTAINER_NAME.equals(getAttributeValue(parser, ATTR_CONTAINER))) {
            out[0] = LauncherSettings.Favorites.CONTAINER_HOTSEAT;
            // Hack: hotseat items are stored using screen ids
            long rank = Long.parseLong(getAttributeValue(parser, ATTR_RANK));
            out[1] = (FeatureFlags.NO_ALL_APPS_ICON || rank < mIdp.getAllAppsButtonRank())
                    ? rank : (rank + 1);
        } else {
            out[0] = LauncherSettings.Favorites.CONTAINER_DESKTOP;
            out[1] = Long.parseLong(getAttributeValue(parser, ATTR_SCREEN));
        }
    }

    /**
     * Parses the current node and returns the number of elements added.
     */
    protected int parseAndAddNode(
            XmlPullParser parser,
            HashMap<String, TagParser> tagParserMap,
            ArrayList<Long> screenIds)
            throws XmlPullParserException, IOException {

        int result = parseInclude(parser, screenIds);
        if (result >= 0) {
            return result;
        }

        mValues.clear();
        parseContainerAndScreen(parser, mTemp);
        final long container = mTemp[0];
        final long screenId = mTemp[1];

        mValues.put(LauncherSettings.Favorites.CONTAINER, container);
        mValues.put(LauncherSettings.Favorites.SCREEN, screenId);

        mValues.put(LauncherSettings.Favorites.CELLX,
                convertToDistanceFromEnd(getAttributeValue(parser, ATTR_X), mColumnCount));
        mValues.put(LauncherSettings.Favorites.CELLY,
                convertToDistanceFromEnd(getAttributeValue(parser, ATTR_Y), mRowCount));

        TagParser tagParser = tagParserMap.get(parser.getName());
        if (tagParser == null) {
            if (LOGD) Log.d(TAG, "Ignoring unknown element tag: " + parser.getName());
            return 0;
        }
        long newElementId = tagParser.parseAndAdd(parser);
        if (newElementId >= 0) {
            // Keep track of the set of screens which need to be added to the db.
            if (!screenIds.contains(screenId) &&
                    container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                screenIds.add(screenId);
            }
            return 1;
        }
        return 0;
    }

    protected long addShortcut(String title, Intent intent, int type) {
        long id = mCallback.generateNewItemId();
        mValues.put(LauncherSettings.Favorites.INTENT, intent.toUri(0));
        mValues.put(LauncherSettings.Favorites.TITLE, title);
        mValues.put(LauncherSettings.Favorites.ITEM_TYPE, type);
        mValues.put(LauncherSettings.Favorites.SPANX, 1);
        mValues.put(LauncherSettings.Favorites.SPANY, 1);
        mValues.put(LauncherSettings.Favorites._ID, id);
        if (mCallback.insertAndCheck(mDb, mValues) < 0) {
            return -1;
        } else {
            return id;
        }
    }

    protected HashMap<String, TagParser> getFolderElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<String, TagParser>();
        parsers.put(TAG_APP_ICON, new AppShortcutParser());
        parsers.put(TAG_AUTO_INSTALL, new AutoInstallParser());
        parsers.put(TAG_SHORTCUT, new ShortcutParser(mSourceRes));
        return parsers;
    }

    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<String, TagParser>();
        parsers.put(TAG_APP_ICON, new AppShortcutParser());
        parsers.put(TAG_AUTO_INSTALL, new AutoInstallParser());
        parsers.put(TAG_FOLDER, new FolderParser());
        parsers.put(TAG_APPWIDGET, new PendingWidgetParser());
        parsers.put(TAG_SHORTCUT, new ShortcutParser(mSourceRes));
        return parsers;
    }

    protected interface TagParser {
        /**
         * Parses the tag and adds to the db
         * @return the id of the row added or -1;
         */
        long parseAndAdd(XmlPullParser parser)
                throws XmlPullParserException, IOException;
    }

    /**
     * App shortcuts: required attributes packageName and className
     */
    protected class AppShortcutParser implements TagParser {

        @Override
        public long parseAndAdd(XmlPullParser parser) {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);

            if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
                ActivityInfo info;
                try {
                    ComponentName cn;
                    try {
                        cn = new ComponentName(packageName, className);
                        info = mPackageManager.getActivityInfo(cn, 0);
                    } catch (PackageManager.NameNotFoundException nnfe) {
                        String[] packages = mPackageManager.currentToCanonicalPackageNames(
                                new String[] { packageName });
                        cn = new ComponentName(packages[0], className);
                        info = mPackageManager.getActivityInfo(cn, 0);
                    }
                    final Intent intent = new Intent(Intent.ACTION_MAIN, null)
                            .addCategory(Intent.CATEGORY_LAUNCHER)
                            .setComponent(cn)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                    return addShortcut(info.loadLabel(mPackageManager).toString(),
                            intent, LauncherSettings.Favorites.ITEM_TYPE_APPLICATION);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Favorite not found: " + packageName + "/" + className);
                }
                return -1;
            } else {
                return invalidPackageOrClass(parser);
            }
        }

        /**
         * Helper method to allow extending the parser capabilities
         */
        protected long invalidPackageOrClass(XmlPullParser parser) {
            Log.w(TAG, "Skipping invalid <favorite> with no component");
            return -1;
        }
    }

    /**
     * AppShortcutParser which also supports adding URI based intents
     */
    public class AppShortcutWithUriParser extends AppShortcutParser {

        @Override
        protected long invalidPackageOrClass(XmlPullParser parser) {
            final String uri = getAttributeValue(parser, ATTR_URI);
            if (TextUtils.isEmpty(uri)) {
                Log.e(TAG, "Skipping invalid <favorite> with no component or uri");
                return -1;
            }

            final Intent metaIntent;
            try {
                metaIntent = Intent.parseUri(uri, 0);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Unable to add meta-favorite: " + uri, e);
                return -1;
            }

            ResolveInfo resolved = mPackageManager.resolveActivity(metaIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            final List<ResolveInfo> appList = mPackageManager.queryIntentActivities(
                    metaIntent, PackageManager.MATCH_DEFAULT_ONLY);

            // Verify that the result is an app and not just the resolver dialog asking which
            // app to use.
            if (wouldLaunchResolverActivity(resolved, appList)) {
                // If only one of the results is a system app then choose that as the default.
                final ResolveInfo systemApp = getSingleSystemActivity(appList);
                if (systemApp == null) {
                    // There is no logical choice for this meta-favorite, so rather than making
                    // a bad choice just add nothing.
                    Log.w(TAG, "No preference or single system activity found for "
                            + metaIntent.toString());
                    return -1;
                }
                resolved = systemApp;
            }
            final ActivityInfo info = resolved.activityInfo;
            final Intent intent = mPackageManager.getLaunchIntentForPackage(info.packageName);
            if (intent == null) {
                return -1;
            }
            ///M: ALPS02377365,intent is different with launched from "all apps list" {@
            if ("com.android.gallery3d".equals(info.packageName)) {
                intent.setPackage(null);
            }
            /// @}
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            return addShortcut(info.loadLabel(mPackageManager).toString(), intent,
                    LauncherSettings.Favorites.ITEM_TYPE_APPLICATION);
        }

        private ResolveInfo getSingleSystemActivity(List<ResolveInfo> appList) {
            ResolveInfo systemResolve = null;
            final int N = appList.size();
            for (int i = 0; i < N; ++i) {
                try {
                    ApplicationInfo info = mPackageManager.getApplicationInfo(
                            appList.get(i).activityInfo.packageName, 0);
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        if (systemResolve != null) {
                            return null;
                        } else {
                            systemResolve = appList.get(i);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Unable to get info about resolve results", e);
                    return null;
                }
            }
            return systemResolve;
        }

        private boolean wouldLaunchResolverActivity(ResolveInfo resolved,
                                                    List<ResolveInfo> appList) {
            // If the list contains the above resolved activity, then it can't be
            // ResolverActivity itself.
            for (int i = 0; i < appList.size(); ++i) {
                ResolveInfo tmp = appList.get(i);
                if (tmp.activityInfo.name.equals(resolved.activityInfo.name)
                        && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Shortcut parser which allows any uri and not just web urls.
     */
    public class UriShortcutParser extends ShortcutParser {

        public UriShortcutParser(Resources iconRes) {
            super(iconRes);
        }

        @Override
        protected Intent parseIntent(XmlPullParser parser) {
            String uri = null;
            try {
                uri = getAttributeValue(parser, ATTR_URI);
                return Intent.parseUri(uri, 0);
            } catch (URISyntaxException e) {
                Log.w(TAG, "Shortcut has malformed uri: " + uri);
                return null; // Oh well
            }
        }
    }

    /**
     * Contains a list of <favorite> nodes, and accepts the first successfully parsed node.
     */
    public class ResolveParser implements TagParser {

        private final AppShortcutWithUriParser mChildParser = new AppShortcutWithUriParser();

        @Override
        public long parseAndAdd(XmlPullParser parser) throws XmlPullParserException,
                IOException {
            final int groupDepth = parser.getDepth();
            int type;
            long addedId = -1;
            while ((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > groupDepth) {
                if (type != XmlPullParser.START_TAG || addedId > -1) {
                    continue;
                }
                final String fallback_item_name = parser.getName();
                if (TAG_FAVORITE.equals(fallback_item_name)) {
                    addedId = mChildParser.parseAndAdd(parser);
                } else {
                    Log.e(TAG, "Fallback groups can contain only favorites, found "
                            + fallback_item_name);
                }
            }
            return addedId;
        }
    }

    /**
     * AutoInstall: required attributes packageName and className
     */
    protected class AutoInstallParser implements TagParser {

        @Override
        public long parseAndAdd(XmlPullParser parser) {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                if (LOGD) Log.d(TAG, "Skipping invalid <favorite> with no component");
                return -1;
            }

            mValues.put(LauncherSettings.Favorites.RESTORED, ShortcutInfo.FLAG_AUTOINSTALL_ICON);
            final Intent intent = new Intent(Intent.ACTION_MAIN, null)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(new ComponentName(packageName, className))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            return addShortcut(mContext.getString(R.string.package_state_unknown), intent,
                    LauncherSettings.Favorites.ITEM_TYPE_APPLICATION);
        }
    }

    /**
     * Parses a web shortcut. Required attributes url, icon, title
     */
    protected class ShortcutParser implements TagParser {

        private final Resources mIconRes;

        public ShortcutParser(Resources iconRes) {
            mIconRes = iconRes;
        }

        @Override
        public long parseAndAdd(XmlPullParser parser) {
            final int titleResId = getAttributeResourceValue(parser, ATTR_TITLE, 0);
            final int iconId = getAttributeResourceValue(parser, ATTR_ICON, 0);

            if (titleResId == 0 || iconId == 0) {
                if (LOGD) Log.d(TAG, "Ignoring shortcut");
                return -1;
            }

            final Intent intent = parseIntent(parser);
            if (intent == null) {
                return -1;
            }

            Drawable icon = mIconRes.getDrawable(iconId);
            if (icon == null) {
                if (LOGD) Log.d(TAG, "Ignoring shortcut, can't load icon");
                return -1;
            }

            LauncherIcons li = LauncherIcons.obtain(mContext);
            mValues.put(LauncherSettings.Favorites.ICON, Utilities.flattenBitmap(
                    li.createBadgedIconBitmap(icon, Process.myUserHandle(), Build.VERSION.SDK_INT,mContext).icon));
            li.recycle();
            mValues.put(LauncherSettings.Favorites.ICON_PACKAGE, mIconRes.getResourcePackageName(iconId));
            mValues.put(LauncherSettings.Favorites.ICON_RESOURCE, mIconRes.getResourceName(iconId));

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            return addShortcut(mSourceRes.getString(titleResId),
                    intent, LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT);
        }

        protected Intent parseIntent(XmlPullParser parser) {
            final String url = getAttributeValue(parser, ATTR_URL);
            if (TextUtils.isEmpty(url) || !Patterns.WEB_URL.matcher(url).matches()) {
                if (LOGD) Log.d(TAG, "Ignoring shortcut, invalid url: " + url);
                return null;
            }
            return new Intent(Intent.ACTION_VIEW, null).setData(Uri.parse(url));
        }
    }

    /**
     * AppWidget parser: Required attributes packageName, className, spanX and spanY.
     * Options child nodes: <extra key=... value=... />
     * It adds a pending widget which allows the widget to come later. If there are extras, those
     * are passed to widget options during bind.
     * The config activity for the widget (if present) is not shown, so any optional configurations
     * should be passed as extras and the widget should support reading these widget options.
     */
    protected class PendingWidgetParser implements TagParser {

        @Override
        public long parseAndAdd(XmlPullParser parser)
                throws XmlPullParserException, IOException {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                if (LOGD) Log.d(TAG, "Skipping invalid <appwidget> with no component");
                return -1;
            }

            mValues.put(LauncherSettings.Favorites.SPANX, getAttributeValue(parser, ATTR_SPAN_X));
            mValues.put(LauncherSettings.Favorites.SPANY, getAttributeValue(parser, ATTR_SPAN_Y));
            mValues.put(LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET);

            // Read the extras
            Bundle extras = new Bundle();
            int widgetDepth = parser.getDepth();
            int type;
            while ((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > widgetDepth) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                if (TAG_EXTRA.equals(parser.getName())) {
                    String key = getAttributeValue(parser, ATTR_KEY);
                    String value = getAttributeValue(parser, ATTR_VALUE);
                    if (key != null && value != null) {
                        extras.putString(key, value);
                    } else {
                        throw new RuntimeException("Widget extras must have a key and value");
                    }
                } else {
                    throw new RuntimeException("Widgets can contain only extras");
                }
            }

            return verifyAndInsert(new ComponentName(packageName, className), extras);
        }

        protected long verifyAndInsert(ComponentName cn, Bundle extras) {
            mValues.put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, cn.flattenToString());
            mValues.put(LauncherSettings.Favorites.RESTORED,
                    LauncherAppWidgetInfo.FLAG_ID_NOT_VALID |
                            LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY |
                            LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG);
            mValues.put(LauncherSettings.Favorites._ID, mCallback.generateNewItemId());
            if (!extras.isEmpty()) {
                mValues.put(LauncherSettings.Favorites.INTENT, new Intent().putExtras(extras).toUri(0));
            }

            long insertedId = mCallback.insertAndCheck(mDb, mValues);
            if (insertedId < 0) {
                return -1;
            } else {
                return insertedId;
            }
        }
    }

    /**
     * AppWidget parser which enforces that the app is already installed when the layout is parsed.
     */
    protected class AppWidgetParser extends PendingWidgetParser {

        @Override
        protected long verifyAndInsert(ComponentName cn, Bundle extras) {
            try {
                mPackageManager.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                String[] packages = mPackageManager.currentToCanonicalPackageNames(
                        new String[] { cn.getPackageName() });
                cn = new ComponentName(packages[0], cn.getClassName());
                try {
                    mPackageManager.getReceiverInfo(cn, 0);
                } catch (Exception e1) {
                    Log.d(TAG, "Can't find widget provider: " + cn.getClassName());
                    return -1;
                }
            }

            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            long insertedId = -1;
            try {
                int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

                if (!appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                    Log.e(TAG, "Unable to bind app widget id " + cn);
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    return -1;
                }

                mValues.put(LauncherSettings.Favorites.APPWIDGET_ID, appWidgetId);
                mValues.put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, cn.flattenToString());
                mValues.put(LauncherSettings.Favorites._ID, mCallback.generateNewItemId());
                insertedId = mCallback.insertAndCheck(mDb, mValues);
                if (insertedId < 0) {
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    return insertedId;
                }

                // Send a broadcast to configure the widget
                if (!extras.isEmpty()) {
                    Intent intent = new Intent(ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE);
                    intent.setComponent(cn);
                    intent.putExtras(extras);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    mContext.sendBroadcast(intent);
                }
            } catch (RuntimeException ex) {
                Log.e(TAG, "Problem allocating appWidgetId", ex);
            }
            return insertedId;
        }
    }

    protected class FolderParser implements TagParser {
        private final HashMap<String, TagParser> mFolderElements;

        public FolderParser() {
            this(getFolderElementsMap());
        }

        public FolderParser(HashMap<String, TagParser> elements) {
            mFolderElements = elements;
        }

        @Override
        public long parseAndAdd(XmlPullParser parser)
                throws XmlPullParserException, IOException {
            final String title;
            final int titleResId = getAttributeResourceValue(parser, ATTR_TITLE, 0);
            if (titleResId != 0) {
                title = mSourceRes.getString(titleResId);
            } else {
                title = getAttributeValue(parser, ATTR_TITLE);
            }

            mValues.put(LauncherSettings.Favorites.TITLE, title);
            mValues.put(LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.ITEM_TYPE_FOLDER);
            mValues.put(LauncherSettings.Favorites.SPANX, 1);
            mValues.put(LauncherSettings.Favorites.SPANY, 1);
            mValues.put(LauncherSettings.Favorites._ID, mCallback.generateNewItemId());
            long folderId = mCallback.insertAndCheck(mDb, mValues);
            if (folderId < 0) {
                if (LOGD) Log.e(TAG, "Unable to add folder");
                return -1;
            }

            final ContentValues myValues = new ContentValues(mValues);
            ArrayList<Long> folderItems = new ArrayList<Long>();

            int type;
            int folderDepth = parser.getDepth();
            int rank = 0;
            while ((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > folderDepth) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                mValues.clear();
                mValues.put(LauncherSettings.Favorites.CONTAINER, folderId);
                mValues.put(LauncherSettings.Favorites.RANK, rank);

                TagParser tagParser = mFolderElements.get(parser.getName());
                if (tagParser != null) {
                    final long id = tagParser.parseAndAdd(parser);
                    if (id >= 0) {
                        folderItems.add(id);
                        rank++;
                    }
                } else {
                    throw new RuntimeException("Invalid folder item " + parser.getName());
                }
            }

            long addedId = folderId;

            // We can only have folders with >= 2 items, so we need to remove the
            // folder and clean up if less than 2 items were included, or some
            // failed to add, and less than 2 were actually added
            if (folderItems.size() < 2) {
                // Delete the folder
                Uri uri = LauncherSettings.Favorites.getContentUri(folderId);
                LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, null, null);
                mDb.delete(args.table, args.where, args.args);
                addedId = -1;

                // If we have a single item, promote it to where the folder
                // would have been.
                if (folderItems.size() == 1) {
                    final ContentValues childValues = new ContentValues();
                    copyInteger(myValues, childValues, LauncherSettings.Favorites.CONTAINER);
                    copyInteger(myValues, childValues, LauncherSettings.Favorites.SCREEN);
                    copyInteger(myValues, childValues, LauncherSettings.Favorites.CELLX);
                    copyInteger(myValues, childValues, LauncherSettings.Favorites.CELLY);

                    addedId = folderItems.get(0);
                    mDb.update(LauncherSettings.Favorites.TABLE_NAME, childValues,
                            LauncherSettings.Favorites._ID + "=" + addedId, null);
                }
            }
            return addedId;
        }
    }

    protected static final void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT);

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    private static String convertToDistanceFromEnd(String value, int endValue) {
        if (!TextUtils.isEmpty(value)) {
            int x = Integer.parseInt(value);
            if (x < 0) {
                return Integer.toString(endValue + x);
            }
        }
        return value;
    }

    /**
     * Return attribute value, attempting launcher-specific namespace first
     * before falling back to anonymous attribute.
     */
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
    protected int getAttributeResourceValue(XmlPullParser parser, String attribute,
                                            int defaultValue) {
        String text = getAttributeValue(parser, attribute);
        if (text.contains(":")) {
            return mSourceRes.getIdentifier(text, null, null);
        } else if (text.startsWith("@")){
            int index = text.indexOf('/');
            String type = text.substring(1, index);
            String name = text.substring(index + 1);
            return mSourceRes.getIdentifier(name, type, mContext.getPackageName());
        }
        return 0;
    }

    public static interface LayoutParserCallback {
        long generateNewItemId();

        long insertAndCheck(SQLiteDatabase db, ContentValues values);
    }

    @Thunk
    static void copyInteger(ContentValues from, ContentValues to, String key) {
        to.put(key, from.getAsInteger(key));
    }
}
