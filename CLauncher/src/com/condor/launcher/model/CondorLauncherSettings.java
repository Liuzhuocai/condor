package com.condor.launcher.model;

import android.net.Uri;

import com.android.launcher3.LauncherSettings;

/**
 * Created by Perry on 19-1-24
 */
public class CondorLauncherSettings {
    public static final class LockedTasks implements LauncherSettings.ChangeLogColumns {
        public static final String TABLE_NAME = "LockedTasks";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" +
                CondorGlobalProvider.AUTHORITY + "/" + TABLE_NAME);


        /**
         * The Component name of the gesture
         * <P>Type: TEXT</P>
         */
        public static final String COMPONENT = "component";

        /**
         * The profile id of the item in the cell.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String PROFILE_ID = "profileId";
    }

    // Perry: Add search recents for all apps: start
    public static final class SearchRecent implements LauncherSettings.ChangeLogColumns {
        public static final String TABLE_NAME = "SearchRecent";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" +
                CondorGlobalProvider.AUTHORITY + "/" + TABLE_NAME);


        /**
         * The Component name of the gesture
         * <P>Type: TEXT</P>
         */
        public static final String COMPONENT = "component";

        /**
         * The profile id of the item in the cell.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String PROFILE_ID = "profileId";

        public static final String ITEM_TYPE = "itemType";
        public static final String ITEM_ID = "itemId";
    }
    // Perry: Add search recents for all apps: end
}
