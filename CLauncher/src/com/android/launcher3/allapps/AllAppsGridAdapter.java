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
import android.content.Intent;
import android.content.res.Resources;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityRecordCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.Themes;
import com.condor.launcher.graphics.CaretDrawable;

import java.util.List;

import static com.condor.launcher.search.SearchKey.SEARCH_TYPE_APP;
import static com.condor.launcher.search.SearchKey.SEARCH_TYPE_RECENT;

/**
 * The grid view adapter of all the apps.
 */
public class AllAppsGridAdapter extends RecyclerView.Adapter<AllAppsGridAdapter.ViewHolder> {

    public static final String TAG = "AppsGridAdapter";

    // A normal icon
    public static final int VIEW_TYPE_ICON = 1 << 1;
    // The message shown when there are no filtered results
    public static final int VIEW_TYPE_EMPTY_SEARCH = 1 << 2;
    // The message to continue to a market search when there are no filtered results
    public static final int VIEW_TYPE_SEARCH_MARKET = 1 << 3;

    // We use various dividers for various purposes.  They share enough attributes to reuse layouts,
    // but differ in enough attributes to require different view types

    // A divider that separates the apps list and the search market button
    public static final int VIEW_TYPE_ALL_APPS_DIVIDER = 1 << 4;
    public static final int VIEW_TYPE_WORK_TAB_FOOTER = 1 << 5;

    // Perry: locate application function: start
    // for search items
    public static final int VIEW_TYPE_SEARCH_RESULT_HEADER = 1 << 6;

    public static final int VIEW_TYPE_SEARCH_APP = 1 << 7;

    // Common view type masks
    public static final int VIEW_TYPE_MASK_DIVIDER = VIEW_TYPE_ALL_APPS_DIVIDER
            | VIEW_TYPE_SEARCH_RESULT_HEADER
            | VIEW_TYPE_SEARCH_APP;

    // Perry: Optimizing the scroll effect of search results: start
    public static final int VIEW_TYPE_MASK_SEARCH_ITEM = VIEW_TYPE_SEARCH_APP;
    // Perry: Optimizing the scroll effect of search results: end
    // Perry: locate application function: end

    public static final int VIEW_TYPE_MASK_ICON = VIEW_TYPE_ICON;


    public interface BindViewCallback {
        void onBindView(ViewHolder holder);
    }

    /**
     * ViewHolder for each icon.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View v) {
            super(v);
        }
    }

    /**
     * A subclass of GridLayoutManager that overrides accessibility values during app search.
     */
    public class AppsGridLayoutManager extends GridLayoutManager {

        public AppsGridLayoutManager(Context context) {
            super(context, 1, GridLayoutManager.VERTICAL, false);
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);

            // Ensure that we only report the number apps for accessibility not including other
            // adapter views
            final AccessibilityRecordCompat record = AccessibilityEventCompat
                    .asRecord(event);
            record.setItemCount(mApps.getNumFilteredApps());
            record.setFromIndex(Math.max(0,
                    record.getFromIndex() - getRowsNotForAccessibility(record.getFromIndex())));
            record.setToIndex(Math.max(0,
                    record.getToIndex() - getRowsNotForAccessibility(record.getToIndex())));
        }

        @Override
        public int getRowCountForAccessibility(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return super.getRowCountForAccessibility(recycler, state) -
                    getRowsNotForAccessibility(mApps.getAdapterItems().size() - 1);
        }

        @Override
        public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler,
                RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);

            ViewGroup.LayoutParams lp = host.getLayoutParams();
            AccessibilityNodeInfoCompat.CollectionItemInfoCompat cic = info.getCollectionItemInfo();
            if (!(lp instanceof LayoutParams) || (cic == null)) {
                return;
            }
            LayoutParams glp = (LayoutParams) lp;
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                    cic.getRowIndex() - getRowsNotForAccessibility(glp.getViewAdapterPosition()),
                    cic.getRowSpan(),
                    cic.getColumnIndex(),
                    cic.getColumnSpan(),
                    cic.isHeading(),
                    cic.isSelected()));
        }

        /**
         * Returns the number of rows before {@param adapterPosition}, including this position
         * which should not be counted towards the collection info.
         */
        private int getRowsNotForAccessibility(int adapterPosition) {
            List<AdapterItem> items = mApps.getAdapterItems();
            adapterPosition = Math.max(adapterPosition, mApps.getAdapterItems().size() - 1);
            int extraRows = 0;
            for (int i = 0; i <= adapterPosition; i++) {
                if (!isViewType(items.get(i).viewType, VIEW_TYPE_MASK_ICON)) {
                    extraRows++;
                }
            }
            return extraRows;
        }
    }

    /**
     * Helper class to size the grid items.
     */
    public class GridSpanSizer extends GridLayoutManager.SpanSizeLookup {

        public GridSpanSizer() {
            super();
            setSpanIndexCacheEnabled(true);
        }

        @Override
        public int getSpanSize(int position) {
            if (isIconViewType(mApps.getAdapterItems().get(position).viewType)) {
                return 1;
            } else {
                // Section breaks span the full width
                return mAppsPerRow;
            }
        }
    }

    private final Launcher mLauncher;
    private final LayoutInflater mLayoutInflater;
    private final AlphabeticalAppsList mApps;
    private final GridLayoutManager mGridLayoutMgr;
    private final GridSpanSizer mGridSizer;

    private final int mAppsPerRow;

    private BindViewCallback mBindViewCallback;
    private OnFocusChangeListener mIconFocusListener;

    // The text to show when there are no search results and no market search handler.
    private String mEmptySearchMessage;
    // The intent to send off to the market app, updated each time the search query changes.
    private Intent mMarketSearchIntent;

    private int mFoldUpFilterTypes;

    public AllAppsGridAdapter(Launcher launcher, AlphabeticalAppsList apps) {
        Resources res = launcher.getResources();
        mLauncher = launcher;
        mApps = apps;
        mEmptySearchMessage = res.getString(R.string.all_apps_loading_message);
        mGridSizer = new GridSpanSizer();
        mGridLayoutMgr = new AppsGridLayoutManager(launcher);
        mGridLayoutMgr.setSpanSizeLookup(mGridSizer);
        mLayoutInflater = LayoutInflater.from(launcher);

        mAppsPerRow = mLauncher.getDeviceProfile().inv.numColumns;
        mGridLayoutMgr.setSpanCount(mAppsPerRow);
    }

    public static boolean isDividerViewType(int viewType) {
        return isViewType(viewType, VIEW_TYPE_MASK_DIVIDER);
    }

    // Perry: Optimizing the scroll effect of search results: start
    public static boolean isSearchItemViewType(int viewType) {
        return isViewType(viewType, VIEW_TYPE_MASK_SEARCH_ITEM);
    }
    // Perry: Optimizing the scroll effect of search results: end

    public static boolean isIconViewType(int viewType) {
        return isViewType(viewType, VIEW_TYPE_MASK_ICON);
    }

    public static boolean isViewType(int viewType, int viewTypeMask) {
        return (viewType & viewTypeMask) != 0;
    }

    public void setIconFocusListener(OnFocusChangeListener focusListener) {
        mIconFocusListener = focusListener;
    }

    /**
     * Sets the last search query that was made, used to show when there are no results and to also
     * seed the intent for searching the market.
     */
    public void setLastSearchQuery(String query) {
        Resources res = mLauncher.getResources();
        mEmptySearchMessage = res.getString(R.string.all_apps_no_search_results, query);
        mMarketSearchIntent = PackageManagerHelper.getMarketSearchIntent(mLauncher, query);
    }

    /**
     * Sets the callback for when views are bound.
     */
    public void setBindViewCallback(BindViewCallback cb) {
        mBindViewCallback = cb;
    }

    /**
     * Returns the grid layout manager.
     */
    public GridLayoutManager getLayoutManager() {
        return mGridLayoutMgr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ICON: {
                BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                        R.layout.all_apps_icon, parent, false);
                icon.setOnClickListener(ItemClickHandler.INSTANCE);
                icon.setOnLongClickListener(ItemLongClickListener.INSTANCE_ALL_APPS);
                icon.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
                icon.setOnFocusChangeListener(mIconFocusListener);

                // Ensure the all apps icon height matches the workspace icons in portrait mode.
                icon.getLayoutParams().height = mLauncher.getDeviceProfile().allAppsCellHeightPx;
                return new ViewHolder(icon);
            }
            case VIEW_TYPE_EMPTY_SEARCH:
                return new ViewHolder(mLayoutInflater.inflate(R.layout.all_apps_empty_search,
                        parent, false));
            case VIEW_TYPE_SEARCH_MARKET:
                View searchMarketView = mLayoutInflater.inflate(R.layout.all_apps_search_market,
                        parent, false);
                searchMarketView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mLauncher.startActivitySafely(v, mMarketSearchIntent, null);
                    }
                });
                return new ViewHolder(searchMarketView);
            case VIEW_TYPE_ALL_APPS_DIVIDER:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.all_apps_divider, parent, false));
            case VIEW_TYPE_WORK_TAB_FOOTER:
                View footer = mLayoutInflater.inflate(R.layout.work_tab_footer, parent, false);
                return new ViewHolder(footer);
            case VIEW_TYPE_SEARCH_RESULT_HEADER: {
                View root = mLayoutInflater.inflate(R.layout.all_apps_search_result_header, parent, false);
                View foldUp = root.findViewById(R.id.search_result_fold_up_layout);
                // Perry: Add search recents for all apps: start
                TextView button = root.findViewById(R.id.search_result_button);
                button.setOnClickListener(v-> mApps.clearRecentItems());
                // Perry: Add search recents for all apps: end
                ImageView foldUpArrow = root.findViewById(R.id.search_result_fold_up_arrow);
                foldUpArrow.setImageDrawable(new CaretDrawable(mLauncher, false));
                foldUp.setOnClickListener(v-> {
                    toggleFoldUpFilter((int) v.getTag());
                    mLauncher.hideSortInput();
                });
                return new ViewHolder(root);
            }
            case VIEW_TYPE_SEARCH_APP: {
                View root = mLayoutInflater.inflate(R.layout.all_apps_search_app, parent, false);
                // Perry: Add search recents for all apps: start
                final View.OnClickListener clickListener = v -> {
                    ItemClickHandler.INSTANCE.onClick(v);
                    mApps.addRecentItem((AppInfo) v.getTag());
                };
                // Perry: Add search recents for all apps: end
                root.setOnClickListener(clickListener);
                root.setFocusable(true);
                root.setOnFocusChangeListener(mIconFocusListener);
                final BubbleTextView icon = root.findViewById(R.id.search_app_icon);
                icon.setOnClickListener(clickListener);
                icon.setOnLongClickListener(ItemLongClickListener.INSTANCE_ALL_APPS);
                icon.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
                ImageView location = root.findViewById(R.id.search_app_location);
                location.setOnClickListener(v-> {
                    final AppInfo info = (AppInfo) icon.getTag();
                    final BubbleTextView app = mLauncher.getWorkspace().findShortcutByAppInfo(info);
                    if (app != null) {
                        mLauncher.hideSortInput();
                        mLauncher.getStateManager().goToState(LauncherState.NORMAL, true,
                                ()-> mLauncher.getWorkspace().location(app));
                        return;
                    }

                    mLauncher.getAppsView().location(info);
                });
                return new ViewHolder(root);
            }
            default:
                throw new RuntimeException("Unexpected view type");
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_ICON: {
                AppInfo info = mApps.getAdapterItems().get(position).appInfo;
                BubbleTextView icon = (BubbleTextView) holder.itemView;
                icon.reset();
                icon.applyFromApplicationInfo(info);
                // Perry: Adjust recents UI: start
                if (mApps.isItemInOverview(position)) {
                    icon.setTextAlpha(mApps.getOverviewProgress());
                }
                // Perry: Adjust recents UI: end
                break;
            }
            case VIEW_TYPE_EMPTY_SEARCH:
                TextView emptyViewText = (TextView) holder.itemView;
                emptyViewText.setText(mEmptySearchMessage);
                emptyViewText.setGravity(mApps.hasNoFilteredResults() ? Gravity.CENTER :
                        Gravity.START | Gravity.CENTER_VERTICAL);
                break;
            case VIEW_TYPE_SEARCH_MARKET:
                TextView searchView = (TextView) holder.itemView;
                if (mMarketSearchIntent != null) {
                    searchView.setVisibility(View.VISIBLE);
                } else {
                    searchView.setVisibility(View.GONE);
                }
                break;
            case VIEW_TYPE_ALL_APPS_DIVIDER:
                // Perry: Adjust recents UI: start
                if (mApps.isItemInOverview(position)) {
                    holder.itemView.setAlpha(mApps.getOverviewProgress());
                }
                // Perry: Adjust recents UI: end
                break;
            case VIEW_TYPE_WORK_TAB_FOOTER:
                WorkModeSwitch workModeToggle = holder.itemView.findViewById(R.id.work_mode_toggle);
                workModeToggle.refresh();
                TextView managedByLabel = holder.itemView.findViewById(R.id.managed_by_label);
                boolean anyProfileQuietModeEnabled = UserManagerCompat.getInstance(
                        managedByLabel.getContext()).isAnyProfileQuietModeEnabled();
                managedByLabel.setText(anyProfileQuietModeEnabled
                        ? R.string.work_mode_off_label : R.string.work_mode_on_label);
                break;
            case VIEW_TYPE_SEARCH_RESULT_HEADER: {
                AlphabeticalAppsList.AdapterItem item = mApps.getAdapterItems().get(position);
                int searchType = item.searchType;
                boolean showDivider = item.showDivider;
                AlphabeticalAppsList.FoldUpStatus status = item.foldUpStatus;
                View itemView = holder.itemView;
                ImageView divider     = itemView.findViewById(R.id.search_result_divider);
                TextView text         = itemView.findViewById(R.id.search_result_type);
                TextView button       = itemView.findViewById(R.id.search_result_button);
                View foldUpLayout     = itemView.findViewById(R.id.search_result_fold_up_layout);
                ImageView foldUpArrow = itemView.findViewById(R.id.search_result_fold_up_arrow);
                TextView foldUpText   = itemView.findViewById(R.id.search_result_fold_up_text);
                CaretDrawable drawable= (CaretDrawable) (foldUpArrow.getDrawable());
                drawable.setCaretColor(Themes.getAttrColor(mLauncher, android.R.attr.textColorSecondary),
                        0);

                divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
                // Perry: Add search recents for all apps: start
                if (searchType == SEARCH_TYPE_RECENT) {
                    button.setVisibility(View.VISIBLE);
                    foldUpLayout.setVisibility(View.GONE);
                    button.setText(R.string.all_apps_search_result_button_clear);
                } else {
                    button.setVisibility(View.GONE);
                    if (status == AlphabeticalAppsList.FoldUpStatus.NONE) {
                        foldUpLayout.setVisibility(View.GONE);
                    } else {
                        foldUpLayout.setVisibility(View.VISIBLE);
                        if (status == AlphabeticalAppsList.FoldUpStatus.EXPAND) {
                            drawable.setCaretProgress(CaretDrawable.
                                    PROGRESS_CARET_POINTING_UP);
                            foldUpText.setText(R.string.fold_up);
                        } else if (status == AlphabeticalAppsList.FoldUpStatus.FOLD_UP) {
                            drawable.setCaretProgress(CaretDrawable.
                                    PROGRESS_CARET_POINTING_DOWN);
                            foldUpText.setText(R.string.expand);
                        }
                        foldUpLayout.setTag(searchType);
                    }
                }
                // Perry: Add search recents for all apps: end

                setFilterText(text, searchType);
                break;
            }
            case VIEW_TYPE_SEARCH_APP: {
                AppInfo searchAppInfo = mApps.getAdapterItems().get(position).appInfo;
                View itemView = holder.itemView;
                BubbleTextView icon = itemView.findViewById(R.id.search_app_icon);
                // Perry: Add highlighter text for search results: start
                icon.applyFromApplicationInfo(searchAppInfo, mApps.getHighlighterText(
                        searchAppInfo.title.toString()));
                // Perry: Add highlighter text for search results: end
                icon.forceHideBadge(true);
                icon.reset();
                itemView.setTag(searchAppInfo);
                break;
            }
        }
        if (mBindViewCallback != null) {
            mBindViewCallback.onBindView(holder);
        }
    }

    @Override
    public boolean onFailedToRecycleView(ViewHolder holder) {
        // Always recycle and we will reset the view when it is bound
        return true;
    }

    @Override
    public int getItemCount() {
        return mApps.getAdapterItems().size();
    }

    @Override
    public int getItemViewType(int position) {
        AlphabeticalAppsList.AdapterItem item = mApps.getAdapterItems().get(position);
        return item.viewType;
    }

    private void setFilterText(TextView text, int searchType) {
        switch (searchType) {
            case SEARCH_TYPE_APP:
                text.setText(R.string.all_apps_search_applications);
                break;
            case SEARCH_TYPE_RECENT:
                text.setText(R.string.all_apps_search_recent);
                break;
        }
    }

    public static int getFilterTypes() {
        return SEARCH_TYPE_APP;
    }

    public void updateFoldUpFilterTypes() {
        mFoldUpFilterTypes = getFilterTypes();
    }

    public boolean isSearchTypeFoldUp(int filterType) {
        return (mFoldUpFilterTypes & filterType) == filterType;
    }

    private void toggleFoldUpFilter(int filterType) {
        if (isSearchTypeFoldUp(filterType)) {
            mFoldUpFilterTypes ^= filterType;
        } else {
            mFoldUpFilterTypes |= filterType;
        }
        mApps.asyncUpdateAdapterItems();
    }

}
