package com.condor.launcher.search;


/**
 * Created by Perry on 19-1-15
 */
public class SearchKey {
    public static final int SEARCH_TYPE_APP = 1 << 1;
    // Perry: Add search recents for all apps: start
    public static final int SEARCH_TYPE_RECENT = 1 << 2;
    // Perry: Add search recents for all apps: end
    public static final int TYPES_COUNT = 2;

    public long itemId;
    protected int type;

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SearchKey) {
            SearchKey other = (SearchKey) o;
            return this.itemId == other.itemId &&
                    this.type == other.type;
        }
        return false;
    }
}
