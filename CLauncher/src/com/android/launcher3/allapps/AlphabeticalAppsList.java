/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.launcher3.allapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.AlphabeticIndexCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.LabelComparator;
import com.condor.launcher.search.RecentItemsManager;
import com.condor.launcher.search.SearchKey;
import com.condor.launcher.switcher.desktopmode.DesktopModeHelper;
import com.condor.launcher.util.TextHighlighter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.android.launcher3.allapps.AllAppsContainerView.State.FROZEN;
import static com.android.launcher3.allapps.AllAppsContainerView.State.NORMAL;

/**
 * The alphabetically sorted list of applications.
 */
public class AlphabeticalAppsList implements AllAppsStore.OnUpdateListener {

    public static final String TAG = "AlphabeticalAppsList";

    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_ROWS_FRACTION = 0;
    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS = 1;

    private final int mFastScrollDistributionMode = FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS;

    // Perry: locate application function: start
    // for search items
    public static final int SEARCH_ITEMS_FOLD_UP_COUNT = 4;

    public enum FoldUpStatus {
        NONE,
        FOLD_UP,
        EXPAND
    }
    // Perry: locate application function: end

    /**
     * Info about a fast scroller section, depending if sections are merged, the fast scroller
     * sections will not be the same set as the section headers.
     */
    public static class FastScrollSectionInfo {
        // The section name
        public String sectionName;
        // The AdapterItem to scroll to for this section
        public AdapterItem fastScrollToItem;
        // The touch fraction that should map to this fast scroll section info
        public float touchFraction;

        public FastScrollSectionInfo(String sectionName) {
            this.sectionName = sectionName;
        }
    }

    /**
     * Info about a particular adapter item (can be either section or app)
     */
    public static class AdapterItem {
        /** Common properties */
        // The index of this adapter item in the list
        public int position;
        // The type of this item
        public int viewType;

        /** App-only properties */
        // The section name of this app.  Note that there can be multiple items with different
        // sectionNames in the same section
        public String sectionName = null;
        // The row that this item shows up on
        public int rowIndex;
        // The index of this app in the row
        public int rowAppIndex;
        // The associated AppInfo for the app
        public AppInfo appInfo = null;
        // The index of this app not including sections
        public int appIndex = -1;

        // Perry: locate application function: start
        // For search items
        public SearchKey key = null;
        public int searchType;
        public boolean showDivider;
        public FoldUpStatus foldUpStatus;
        // Perry: locate application function: end

        public static AdapterItem asApp(int pos, String sectionName, AppInfo appInfo,
                int appIndex) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_ICON;
            item.position = pos;
            item.sectionName = sectionName;
            item.appInfo = appInfo;
            item.appIndex = appIndex;
            return item;
        }

        public static AdapterItem asEmptySearch(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_EMPTY_SEARCH;
            item.position = pos;
            return item;
        }

        public static AdapterItem asAllAppsDivider(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_ALL_APPS_DIVIDER;
            item.position = pos;
            return item;
        }

        public static AdapterItem asMarketSearch(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_SEARCH_MARKET;
            item.position = pos;
            return item;
        }

        public static AdapterItem asWorkTabFooter(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_WORK_TAB_FOOTER;
            item.position = pos;
            return item;
        }

        // Perry: locate application function: start
        public static AdapterItem asSearchResultHeader(int pos, int searchType, boolean showDivider, boolean isFoldUp, int count) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_SEARCH_RESULT_HEADER;
            item.position = pos;
            item.searchType = searchType;
            item.showDivider = showDivider;
            if (count >= SEARCH_ITEMS_FOLD_UP_COUNT) {
                if (isFoldUp) {
                    item.foldUpStatus = FoldUpStatus.FOLD_UP;
                } else {
                    item.foldUpStatus = FoldUpStatus.EXPAND;
                }
            } else {
                item.foldUpStatus = FoldUpStatus.NONE;
            }
            return item;
        }

        public static AdapterItem asSearchApp(int pos, String sectionName, AppInfo appInfo,
                                              int appIndex) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_SEARCH_APP;
            item.position = pos;
            item.sectionName = sectionName;
            item.appInfo = appInfo;
            item.appIndex = appIndex;
            return item;
        }
        // Perry: locate application function: end

    }

    private final Launcher mLauncher;

    // The set of apps from the system
    private final List<AppInfo> mApps = new ArrayList<>();
    private final AllAppsStore mAllAppsStore;

    // The set of filtered apps with the current filter
    // Perry: locate application function: start
    private final List<SearchKey> mFilteredItems = new ArrayList<>();
    // Perry: locate application function: end
    // The current set of adapter items
    private final ArrayList<AdapterItem> mAdapterItems = new ArrayList<>();
    // The set of sections that we allow fast-scrolling to (includes non-merged sections)
    private final List<FastScrollSectionInfo> mFastScrollerSections = new ArrayList<>();
    // Is it the work profile app list.
    private final boolean mIsWork;
    // Perry: Add predicted applications: start
    // The set of predicted app component names
    private final List<ComponentKey> mPredictedAppComponents = new ArrayList<>();
    // The set of predicted apps resolved from the component names and the current set of apps
    private final List<AppInfo> mPredictedApps = new ArrayList<>();
    // Perry: Add predicted applications: end

    // The of ordered component names as a result of a search query
    private ArrayList<SearchKey> mSearchResults;
    private HashMap<CharSequence, String> mCachedSectionNames = new HashMap<>();
    private AllAppsGridAdapter mAdapter;
    private AlphabeticIndexCompat mIndexer;
    private AppInfoComparator mAppNameComparator;
    private final int mNumAppsPerRow;
    private int mNumAppRowsInAdapter;
    private ItemInfoMatcher mItemFilter;
    // Perry: locate application function: start
    private final Runnable mUpdateAdapterItemTask = this::updateAdapterItems;
    // Perry: locate application function: end
    // Perry: Add highlighter text for search results: start
    private final LinkedHashMap<String, TextHighlighter> mHighlighters = new LinkedHashMap<>();
    // Perry: Add highlighter text for search results: end
    // Perry: Adjust recents UI: start
    private float mProgress = 1f;
    // Perry: Adjust recents UI: end
    // Perry: Add search recents for all apps: start
    private final RecentItemsManager mRecentItemsManager = RecentItemsManager.getInstance();
    // Perry: Add search recents for all apps: end

    public AlphabeticalAppsList(Context context, AllAppsStore appsStore, boolean isWork) {
        mAllAppsStore = appsStore;
        mLauncher = Launcher.getLauncher(context);
        mIndexer = new AlphabeticIndexCompat(context);
        mAppNameComparator = new AppInfoComparator(context);
        mIsWork = isWork;
        mNumAppsPerRow = mLauncher.getDeviceProfile().inv.numColumns;
        mAllAppsStore.addUpdateListener(this);
    }

    public void updateItemFilter(ItemInfoMatcher itemFilter) {
        this.mItemFilter = itemFilter;
        onAppsUpdated();
    }

    /**
     * Sets the adapter to notify when this dataset changes.
     */
    public void setAdapter(AllAppsGridAdapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Returns all the apps.
     */
    public List<AppInfo> getApps() {
        return mApps;
    }

    /**
     * Returns fast scroller sections of all the current filtered applications.
     */
    public List<FastScrollSectionInfo> getFastScrollerSections() {
        return mFastScrollerSections;
    }

    /**
     * Returns the current filtered list of applications broken down into their sections.
     */
    public List<AdapterItem> getAdapterItems() {
        return mAdapterItems;
    }

    /**
     * Returns the number of rows of applications
     */
    public int getNumAppRows() {
        return mNumAppRowsInAdapter;
    }

    /**
     * Returns the number of applications in this list.
     */
    public int getNumFilteredApps() {
        return mFilteredItems.size();
    }

    /**
     * Returns whether there are is a filter set.
     */
    public boolean hasFilter() {
        return (mSearchResults != null);
    }

    /**
     * Returns whether there are no filtered results.
     */
    public boolean hasNoFilteredResults() {
        return (mSearchResults != null) && mFilteredItems.isEmpty();
    }

    /**
     * Sets the sorted list of filtered components.
     */
    public boolean setOrderedFilter(ArrayList<SearchKey> f) {
        if (mSearchResults != f) {
            boolean same = mSearchResults != null && mSearchResults.equals(f);
            mSearchResults = f;
            // Perry: Add highlighter text for search results: start
            if (f == null) {
                mHighlighters.clear();
            }
            // Perry: Add highlighter text for search results: end
            onAppsUpdated();
            return !same;
        }
        return false;
    }

    /**
     * Updates internals when the set of apps are updated.
     */
    @Override
    public void onAppsUpdated() {
        // Sort the list of apps
        mApps.clear();

        for (AppInfo app : mAllAppsStore.getApps()) {
            if (mItemFilter == null || mItemFilter.matches(app, null) || hasFilter()) {
                mApps.add(app);
            }
        }

        Collections.sort(mApps, mAppNameComparator);

        // As a special case for some languages (currently only Simplified Chinese), we may need to
        // coalesce sections
        Locale curLocale = mLauncher.getResources().getConfiguration().locale;
        boolean localeRequiresSectionSorting = curLocale.equals(Locale.SIMPLIFIED_CHINESE);
        if (localeRequiresSectionSorting) {
            // Compute the section headers. We use a TreeMap with the section name comparator to
            // ensure that the sections are ordered when we iterate over it later
            TreeMap<String, ArrayList<AppInfo>> sectionMap = new TreeMap<>(new LabelComparator());
            for (AppInfo info : mApps) {
                // Add the section to the cache
                String sectionName = getAndUpdateCachedSectionName(info.title);

                // Add it to the mapping
                ArrayList<AppInfo> sectionApps = sectionMap.get(sectionName);
                if (sectionApps == null) {
                    sectionApps = new ArrayList<>();
                    sectionMap.put(sectionName, sectionApps);
                }
                sectionApps.add(info);
            }

            // Add each of the section apps to the list in order
            mApps.clear();
            for (Map.Entry<String, ArrayList<AppInfo>> entry : sectionMap.entrySet()) {
                mApps.addAll(entry.getValue());
            }
        } else {
            // Just compute the section headers for use below
            for (AppInfo info : mApps) {
                // Add the section to the cache
                getAndUpdateCachedSectionName(info.title);
            }
        }

        // Recompose the set of adapter items from the current set of apps
        updateAdapterItems();
    }

    /**
     * Updates the set of filtered apps with the current filter.  At this point, we expect
     * mCachedSectionNames to have been calculated for the set of all apps in mApps.
     */
    public void updateAdapterItems() {
        refillAdapterItems();
        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void refillAdapterItems() {
        String lastSectionName = null;
        FastScrollSectionInfo lastFastScrollerSectionInfo = null;
        int position = 0;
        int appIndex = 0;

        // Prepare to update the list of sections, filtered apps, etc.
        mFilteredItems.clear();
        mFastScrollerSections.clear();
        mAdapterItems.clear();

        // Perry: locate application function: start
        if (hasFilter()) {
            // Perry: Optimizing the scroll effect of search results: start
            // Recreate the filtered and sectioned apps (for convenience for the grid layout) from the
            // ordered set of sections
            List<AppInfo> filterApps = getFiltersAppInfos();
            int count = filterApps.size();
            boolean isFoldUp = mAdapter.isSearchTypeFoldUp(SearchKey.SEARCH_TYPE_APP);
            if (!filterApps.isEmpty()) {
                AdapterItem appItem = AdapterItem.asSearchResultHeader(position++,
                        SearchKey.SEARCH_TYPE_APP, false, isFoldUp, count);
                mAdapterItems.add(appItem);
            }

            for (int i = 0; i < count; i++) {
                if (i >= SEARCH_ITEMS_FOLD_UP_COUNT && isFoldUp) {
                    break;
                }

                AppInfo info = filterApps.get(i);
                String sectionName = getAndUpdateCachedSectionName(info.title);

                // Create a new section if the section names do not match
                if (!sectionName.equals(lastSectionName)) {
                    lastSectionName = sectionName;
                    lastFastScrollerSectionInfo = new FastScrollSectionInfo(sectionName);
                    mFastScrollerSections.add(lastFastScrollerSectionInfo);
                }

                // Create an app item
                AdapterItem appItem = AdapterItem.asSearchApp(position++, sectionName, info, appIndex++);
                if (lastFastScrollerSectionInfo.fastScrollToItem == null) {
                    lastFastScrollerSectionInfo.fastScrollToItem = appItem;
                }
                mAdapterItems.add(appItem);
                mFilteredItems.add(info.toComponentKey());
            }

            // Append the search market item
            if (hasNoFilteredResults()) {
                mAdapterItems.add(AdapterItem.asEmptySearch(position++));
            } else {
                mAdapterItems.add(AdapterItem.asAllAppsDivider(position++));
            }
            mAdapterItems.add(AdapterItem.asMarketSearch(position++));
            // Perry: Optimizing the scroll effect of search results: end
        } else {
            // Perry: Add predicted applications: start
            mPredictedApps.clear();
            if (!mPredictedAppComponents.isEmpty()) {
                mPredictedApps.addAll(processPredictedAppComponents(mPredictedAppComponents));

                if (!mPredictedApps.isEmpty()) {
                    // Add a section for the predictions
                    lastFastScrollerSectionInfo = new FastScrollSectionInfo("");
                    mFastScrollerSections.add(lastFastScrollerSectionInfo);

                    // Add the predicted app items
                    for (AppInfo info : mPredictedApps) {
                        AdapterItem appItem = AdapterItem.asApp(position++, "", info,
                                appIndex++);
                        if (lastFastScrollerSectionInfo.fastScrollToItem == null) {
                            lastFastScrollerSectionInfo.fastScrollToItem = appItem;
                        }
                        mAdapterItems.add(appItem);
                        mFilteredItems.add(info.toComponentKey());
                    }

                    // Perry: Optimizing the interaction between allapps and recents: start
                    if (DesktopModeHelper.isClassicMode()) {
                        mAdapterItems.add(AdapterItem.asAllAppsDivider(position++));
                    }
                    // Perry: Optimizing the interaction between allapps and recents: end
                }
            }
            // Perry: Add predicted applications: end

            // Recreate the filtered and sectioned apps (for convenience for the grid layout) from the
            // ordered set of sections
            // Perry: Optimizing the interaction between allapps and recents: start
            if (DesktopModeHelper.isClassicMode() || isInState(FROZEN)) {
                for (AppInfo info : getFiltersAppInfos()) {
                    String sectionName = getAndUpdateCachedSectionName(info.title);

                    // Create a new section if the section names do not match
                    if (!sectionName.equals(lastSectionName)) {
                        lastSectionName = sectionName;
                        lastFastScrollerSectionInfo = new FastScrollSectionInfo(sectionName);
                        mFastScrollerSections.add(lastFastScrollerSectionInfo);
                    }

                    // Create an app item
                    AdapterItem appItem = AdapterItem.asApp(position++, sectionName, info, appIndex++);
                    if (lastFastScrollerSectionInfo.fastScrollToItem == null) {
                        lastFastScrollerSectionInfo.fastScrollToItem = appItem;
                    }
                    mAdapterItems.add(appItem);
                    mFilteredItems.add(info.toComponentKey());
                }
            } else {
                // Perry: Add search recents for all apps: start
                final List<SearchKey> mRecentItems = mRecentItemsManager.getRecentItems();
                if (!mRecentItems.isEmpty()) {
                    if (!mAdapterItems.isEmpty()) {
                        mAdapterItems.add(AdapterItem.asAllAppsDivider(position++));
                    }

                    AdapterItem appItem = AdapterItem.asSearchResultHeader(position++,
                            SearchKey.SEARCH_TYPE_RECENT, false, false, 0);
                    mAdapterItems.add(appItem);

                    for (SearchKey key : mRecentItems) {
                        String sectionName = null;
                        if (key instanceof ComponentKey) {
                            AppInfo info = mAllAppsStore.getApp(key);
                            if (info != null) {
                                sectionName = getAndUpdateCachedSectionName(info.title);
                                appItem = AdapterItem.asSearchApp(position++, sectionName, info, appIndex++);
                            }
                        }

                        if (appItem != null && sectionName != null) {
                            // Create a new section if the section names do not match
                            if (!sectionName.equals(lastSectionName)) {
                                lastSectionName = sectionName;
                                lastFastScrollerSectionInfo = new FastScrollSectionInfo(sectionName);
                                mFastScrollerSections.add(lastFastScrollerSectionInfo);
                            }

                            if (lastFastScrollerSectionInfo.fastScrollToItem == null) {
                                lastFastScrollerSectionInfo.fastScrollToItem = appItem;
                            }
                            mAdapterItems.add(appItem);
                            mFilteredItems.add(key);
                        }
                    }
                }
                // Perry: Add search recents for all apps: end
            }
            // Perry: Optimizing the interaction between allapps and recents: end
        }
        // Perry: locate application function: end

        if (mNumAppsPerRow != 0) {
            // Update the number of rows in the adapter after we do all the merging (otherwise, we
            // would have to shift the values again)
            int numAppsInSection = 0;
            int numAppsInRow = 0;
            int rowIndex = -1;
            for (AdapterItem item : mAdapterItems) {
                item.rowIndex = 0;
                // Perry: Optimizing the scroll effect of search results: start
                if (AllAppsGridAdapter.isSearchItemViewType(item.viewType)) {
                    numAppsInRow = 0;
                    numAppsInSection++;
                    rowIndex++;
                    item.rowIndex = rowIndex;
                    item.rowAppIndex = numAppsInRow;
                } else if (AllAppsGridAdapter.isDividerViewType(item.viewType)) {
                    numAppsInSection = 0;
                } else if (AllAppsGridAdapter.isIconViewType(item.viewType)) {
                    if (numAppsInSection % mNumAppsPerRow == 0) {
                        numAppsInRow = 0;
                        rowIndex++;
                    }
                    item.rowIndex = rowIndex;
                    item.rowAppIndex = numAppsInRow;
                    numAppsInSection++;
                    numAppsInRow++;
                }
                // Perry: Optimizing the scroll effect of search results: end
            }
            mNumAppRowsInAdapter = rowIndex + 1;

            // Pre-calculate all the fast scroller fractions
            switch (mFastScrollDistributionMode) {
                case FAST_SCROLL_FRACTION_DISTRIBUTE_BY_ROWS_FRACTION:
                    float rowFraction = 1f / mNumAppRowsInAdapter;
                    for (FastScrollSectionInfo info : mFastScrollerSections) {
                        AdapterItem item = info.fastScrollToItem;
                        if (!AllAppsGridAdapter.isIconViewType(item.viewType)) {
                            info.touchFraction = 0f;
                            continue;
                        }

                        float subRowFraction = item.rowAppIndex * (rowFraction / mNumAppsPerRow);
                        info.touchFraction = item.rowIndex * rowFraction + subRowFraction;
                    }
                    break;
                case FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS:
                    float perSectionTouchFraction = 1f / mFastScrollerSections.size();
                    float cumulativeTouchFraction = 0f;
                    for (FastScrollSectionInfo info : mFastScrollerSections) {
                        AdapterItem item = info.fastScrollToItem;
                        if (!AllAppsGridAdapter.isIconViewType(item.viewType)) {
                            info.touchFraction = 0f;
                            continue;
                        }
                        info.touchFraction = cumulativeTouchFraction;
                        cumulativeTouchFraction += perSectionTouchFraction;
                    }
                    break;
            }
        }

        // Add the work profile footer if required.
        if (shouldShowWorkFooter()) {
            mAdapterItems.add(AdapterItem.asWorkTabFooter(position++));
        }
    }

    private boolean shouldShowWorkFooter() {
        return mIsWork && Utilities.ATLEAST_P &&
                (DeepShortcutManager.getInstance(mLauncher).hasHostPermission()
                        || mLauncher.checkSelfPermission("android.permission.MODIFY_QUIET_MODE")
                        == PackageManager.PERMISSION_GRANTED);
    }

    private List<AppInfo> getFiltersAppInfos() {
        if (mSearchResults == null) {
            // Perry: Implement frozen apps: start
            return mApps.stream().filter(mAllAppsStore.getFilter()).
                    collect(Collectors.toList());
            // Perry: Implement frozen apps: end
        }
        ArrayList<AppInfo> result = new ArrayList<>();
        for (SearchKey key : mSearchResults) {
            AppInfo match = mAllAppsStore.getApp(key);
            if (match != null) {
                result.add(match);
            }
        }
        return result;
    }

    /**
     * Returns the cached section name for the given title, recomputing and updating the cache if
     * the title has no cached section name.
     */
    private String getAndUpdateCachedSectionName(CharSequence title) {
        String sectionName = mCachedSectionNames.get(title);
        if (sectionName == null) {
            sectionName = mIndexer.computeSectionName(title);
            mCachedSectionNames.put(title, sectionName);
        }
        return sectionName;
    }

    // Perry: locate application function: start
    public void asyncUpdateAdapterItems() {
        AllAppsContainerView appsView = mLauncher.getAppsView();
        if (appsView != null) {
            appsView.removeCallbacks(mUpdateAdapterItemTask);
            appsView.post(mUpdateAdapterItemTask);
        }
    }
    // Perry: locate application function: end

    // Perry: Add highlighter text for search results: start
    public void setHighlighters(String s) {
        mHighlighters.clear();
        if (!TextUtils.isEmpty(s)) {
            char[] input = s.trim().toCharArray();
            for (int i = 0; i < input.length; i++) {
                String key = String.valueOf(input[i]).toLowerCase();
                if (!mHighlighters.containsKey(key)) {
                    mHighlighters.put(key, new TextHighlighter()
                            .setForegroundColor(Color.BLUE)
                            .setUnderline(true)
                            .setHighlightedText(key)
                            .setMatcher(TextHighlighter.CASE_INSENSITIVE_MATCHER));
                }
            }
        }
    }

    public Spannable getHighlighterText(String text) {
        Spannable spannable = null;
        String keyword = null;
        for (Map.Entry<String, TextHighlighter> entry : mHighlighters.entrySet()) {
            if (spannable == null) {
                spannable = entry.getValue().getHighlightedText(text);
            } else {
                int index = text.toLowerCase().indexOf(keyword);
                spannable = entry.getValue().getHighlightedText(spannable, text, index > 0 ? index : 0);
            }
            keyword = entry.getKey();
        }
        return spannable;
    }
    // Perry: Add highlighter text for search results: end

    // Perry: Add predicted applications: start
    public void setPredictedApps(List<ComponentKey> apps) {
        mPredictedAppComponents.clear();
        mPredictedAppComponents.addAll(apps);

        List<AppInfo> newPredictedApps = processPredictedAppComponents(apps);
        // We only need to do work if any of the visible predicted apps have changed.
        if (!newPredictedApps.equals(mPredictedApps)) {
            if (newPredictedApps.size() == mPredictedApps.size()) {
                swapInNewPredictedApps(newPredictedApps);
            } else {
                // We need to update the appIndex of all the items.
                onAppsUpdated();
            }
        }
    }

    private List<AppInfo> processPredictedAppComponents(List<ComponentKey> components) {
        // Perry: Implement frozen apps: start
        if (isInState(FROZEN) || mApps.isEmpty()) {
            // Apps have not been bound yet.
            return Collections.emptyList();
        }
        // Perry: Implement frozen apps: end

        List<AppInfo> predictedApps = new ArrayList<>();
        for (SearchKey component : components) {
            AppInfo info = mAllAppsStore.getApp(component);
            if (info != null) {
                predictedApps.add(info);
            } else {
                if (FeatureFlags.IS_DOGFOOD_BUILD) {
                    Log.e(TAG, "Predicted app not found: " + component);
                }
            }
            // Stop at the number of predicted apps
            if (predictedApps.size() == mNumAppsPerRow) {
                break;
            }
        }
        return predictedApps;
    }

    private void swapInNewPredictedApps(List<AppInfo> apps) {
        if (hasFilter()) {
            return;
        }

        mPredictedApps.clear();
        mPredictedApps.addAll(apps);

        int size = apps.size();
        for (int i = 0; i < size; ++i) {
            AppInfo info = apps.get(i);
            AdapterItem appItem = AdapterItem.asApp(i, "", info, i);
            appItem.rowAppIndex = i;
            mAdapterItems.set(i, appItem);
            mFilteredItems.set(i, info.toComponentKey());
            mAdapter.notifyItemChanged(i);
        }
    }

    public List<AppInfo> getPredictedApps() {
        return mPredictedApps;
    }
    // Perry: Add predicted applications: end

    // Perry: Adjust recents UI: start
    public void setOverviewProgress(float progress) {
        mProgress = progress;
    }

    public float getOverviewProgress() {
        return mProgress;
    }

    public boolean isItemInOverview(int position) {
        if (mPredictedApps.isEmpty()) {
            return position >= 0 && position < mNumAppsPerRow;
        } else {
            return position >= 0 && position <= Math.min(mNumAppsPerRow, mPredictedApps.size());
        }
    }
    // Perry: Adjust recents UI: end

    // Perry: Add search recents for all apps: start
    public void addRecentItem(AppInfo info) {
        mRecentItemsManager.addRecentItem(mLauncher, this, info);
    }

    public void addRecentItem(SearchKey key) {
        mRecentItemsManager.addRecentItem(mLauncher, this, key);
    }

    public void clearRecentItems() {
        mRecentItemsManager.clearRecentItems(mLauncher, this);
    }
    // Perry: Add search recents for all apps: end

    // Perry: Implement frozen apps: start
    public boolean isInState(AllAppsContainerView.State state) {
        AllAppsContainerView appsView = mLauncher.getAppsView();

        if (appsView != null) {
            return appsView.isInState(state);
        }

        return state == NORMAL;
    }
    // Perry: Implement frozen apps: end
}
