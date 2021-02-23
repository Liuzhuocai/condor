package com.condor.launcher.search;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.android.launcher3.AppInfo;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.search.AllAppsSearchBarController;
import com.android.launcher3.allapps.search.SearchAlgorithm;
import com.android.launcher3.util.ComponentKey;
import com.condor.launcher.util.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Perry on 19-1-15
 */
public class FuzzySearchAlgorithm implements SearchAlgorithm {
    private static final String TAG = "FuzzySearchAlgorithm";

    private final Context mContext;
    private AlphabeticalAppsList mApps;
    private final HandlerThread mHandlerThread;
    private final Handler mSearchHandler;
    private final Handler mResultHandler;

    public FuzzySearchAlgorithm(Context context, AlphabeticalAppsList apps) {
        mContext = context;
        mApps = apps;
        mResultHandler = new Handler();
        mHandlerThread = ThreadUtils.get(TAG);
        mHandlerThread.start();
        mSearchHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public void cancel(boolean interruptActiveRequests) {
        if (interruptActiveRequests) {
            mSearchHandler.removeCallbacksAndMessages(null);
            mResultHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void doSearch(final String query,
                         final AllAppsSearchBarController.Callbacks callback) {
        final List<AppInfo> apps = new ArrayList<>(mApps.getApps());
        mSearchHandler.removeCallbacksAndMessages(null);
        mSearchHandler.post(()-> {
            final ArrayList<SearchKey> result = getTitleMatchResult(query, apps);
            // Perry: Add highlighter text for search results: start
            mApps.setHighlighters(result.isEmpty() ? null : query);
            // Perry: Add highlighter text for search results: end
            mResultHandler.post(()-> {
                callback.onSearchResult(query, result);
            });
        });
    }

    protected ArrayList<SearchKey> getTitleMatchResult(String query, List<AppInfo> apps) {
        // Do an intersection of the words in the query and each title, and filter out all the
        // apps that don't match all of the words in the query.
        FuzzyMatcher.KeyInfo keyInfo = FuzzyMatcher.getKeyInfo(query.toLowerCase());
        FuzzyMatcher.MatcherInfo matcherInfo = new FuzzyMatcher.MatcherInfo();
        final ArrayList<SearchKey> result = new ArrayList<>();
        for (AppInfo info : apps) {
            matcherInfo.title = info.title.toString();
            matcherInfo.spellName = FuzzyMatcher.toLatin(matcherInfo.title);
            int keyIndex = FuzzyMatcher.indexOf(keyInfo, matcherInfo);
            if (keyIndex < 0) {
                continue;
            }

            MatcherComponentKey key = new MatcherComponentKey(info);
            key.setKeyIndex(keyIndex);
            key.setSpellName(matcherInfo.spellName.toUpperCase());

            result.add(key);
        }

        result.sort((k1, k2) -> {
            MatcherComponentKey i1 = (MatcherComponentKey) k1;
            MatcherComponentKey i2 = (MatcherComponentKey) k2;

            if (i1.getKeyIndex() > i2.getKeyIndex()) {
                return 1;
            } else if (i1.getKeyIndex() == i2.getKeyIndex()) {
                return i1.getSpellName().compareTo(i2.getSpellName());
            } else {
                return -1;
            }
        });

        return result;
    }

    public static List<AppInfo> match(String query, List<AppInfo> apps) {
        FuzzyMatcher.KeyInfo keyInfo = FuzzyMatcher.getKeyInfo(query.toLowerCase());
        FuzzyMatcher.MatcherInfo matcherInfo = new FuzzyMatcher.MatcherInfo();
        final ArrayList<AppInfo> result = new ArrayList<>();
        for (AppInfo info : apps) {
            matcherInfo.title = info.title.toString();
            matcherInfo.spellName = FuzzyMatcher.toLatin(matcherInfo.title);
            int keyIndex = FuzzyMatcher.indexOf(keyInfo, matcherInfo);
            if (keyIndex >= 0) {
                result.add(info);
            }
        }

        return result;
    }

    class MatcherComponentKey extends ComponentKey {
        private String spellName;
        private int keyIndex;

        public MatcherComponentKey(AppInfo info) {
            super(info.componentName, info.user);
        }

        public void setSpellName(String spellName) {
            this.spellName = spellName;
        }

        public void setKeyIndex(int keyIndex) {
            this.keyIndex = keyIndex;
        }

        public String getSpellName() {
            return spellName;
        }

        public int getKeyIndex() {
            return keyIndex;
        }
    }
}
