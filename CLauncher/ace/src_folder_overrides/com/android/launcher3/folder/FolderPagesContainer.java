package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Insettable;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;

import java.util.ArrayList;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * Created by xiaopeng on 17-8-31.
 */

public class FolderPagesContainer extends AbstractFloatingView implements Insettable {
    private static final String TAG = "FolderPagesContainer";
    private FolderViewPager mPager;
    private FolderTabStrip mLabel;
    private final Launcher mLauncher;
    private final ArrayList<Folder> mFolders = new ArrayList<>();
    private Folder mSelectedFolder;
    private boolean mIsHovering;
    // Chenyee <CY_Bug> <xiaopeng> <20181025> for CSW1809A-162 begin
    private boolean mTouchOutSpace;
    // Chenyee <CY_Bug> <xiaopeng> <20181025> for CSW1809A-162 end

    public static FolderPagesContainer create(Context context) {
        return (FolderPagesContainer) LayoutInflater.from(context).
                inflate(R.layout.folder_pages_container, null);
    }

    public FolderPagesContainer(Context context) {
        this(context, null);
    }

    public FolderPagesContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderPagesContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutDirection(LAYOUT_DIRECTION_LTR);

        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    private void initViews() {
        mPager = findViewById(R.id.folder_view_pager);
        mLabel = findViewById(R.id.folder_view_pager_label);
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_SETTLING) {
                    updateSelectedFolder();
                }
            }
        });
        mLabel.setPageChangeListener(mPager);
    }

    private void updateSelectedFolder() {
        Folder folder = getCurrentFolder();
        if (folder != mSelectedFolder) {
            if (mSelectedFolder != null) {
                mSelectedFolder.closeSilent();
            }

            Workspace workspace = mLauncher.getWorkspace();
            if (workspace != null) {
                int pageIndex = workspace.getPageIndexForScreenId(folder.getInfo().screenId);
                workspace.setCurrentPage(pageIndex);
            }
            folder.openSilent();
            mSelectedFolder = folder;
        }
        mPager.setCurrentFolder(folder);
    }

    private void initFolders() {
        ArrayList<CellLayout> layouts = mLauncher.getWorkspace().
                getWorkspaceAndHotseatCellLayouts();
        mFolders.clear();
        for (int i = 0; i < layouts.size(); i++) {
            CellLayout layout = layouts.get(i);
            for (int y = 0; y < layout.getCountY(); y++) {
                for (int x = 0; x < layout.getCountX(); x++) {
                    View view = layout.getChildAt(x, y);
                    if (view instanceof FolderIcon) {
                        FolderIcon folderIcon = (FolderIcon) view;
                        Folder folder = folderIcon.getFolder();
                        if (folder != mSelectedFolder) {
                            folder.scrollToTop();
                        }
                        mFolders.add(folder);
                    }
                }
            }
        }
    }

    public void handleOpen(Folder folder) {
        mIsOpen = true;
        mSelectedFolder = folder;
        initFolders();
        animateShowFolderName();
        mPager.removeAllViews();
        mAdapter.notifyDataSetChanged();
        int position = mFolders.indexOf(folder);
        mPager.setCurrentItem(position);
        mPager.setCurrentFolder(folder);
    }

    public Folder getCurrentFolder() {
        // Chenyee <CY_Bug> <xiaopeng> <20181026> for CSW1805A-1152 begin
        if (mPager.getCurrentItem() >= mFolders.size()) {
            return null;
        }
        // Chenyee <CY_Bug> <xiaopeng> <20181026> for CSW1805A-1152 end

        return mFolders.get(mPager.getCurrentItem());
    }

    public void setNearestFoldersVisible(boolean visible) {
        int alpha = visible ? 1 : 0;
        int currentItem = mPager.getCurrentItem();
        if (currentItem > 0) {
            mFolders.get(currentItem-1).setAlpha(alpha);
        } else if (currentItem < mFolders.size()-1) {
            mFolders.get(currentItem+1).setAlpha(alpha);
        }
    }

    private PagerAdapter mAdapter = new PagerAdapter() {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Folder folder = mFolders.get(position % mFolders.size());
            ((ViewPager) container).addView(folder);
            return folder;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if("".equals(mFolders.get(position).getInfo().title)){
                return getResources().getString(R.string.folder_hint_text);
            }
            return mFolders.get(position).getInfo().title;
        }

        @Override
        public int getCount() {
            return mFolders.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    };

    public void updateCurrentTab() {
        int index = mPager.getCurrentItem();
        mLabel.updatePageTitle(index);
    }

    public void animateClose(Runnable r) {
        Runnable closeRunnable = new Runnable() {
            @Override
            public void run() {
                mIsOpen = false;
                mSelectedFolder = null;
                Folder folder = getCurrentFolder();
                folder.closeFolder(true, r);
            }
        };

        if (mSelectedFolder == null) {
            return;
        }

        if (mSelectedFolder.isAnimating()) {
            mSelectedFolder.setAnimationCompleteRunnable(closeRunnable);
        } else {
            closeRunnable.run();
        }
    }

    @Override
    protected void handleClose(boolean animate) {
        Runnable closeRunnable = new Runnable() {
            @Override
            public void run() {
                mIsOpen = false;
                mSelectedFolder = null;
                Folder folder = getCurrentFolder();
                folder.closeFolder(animate);
            }
        };

        if (mSelectedFolder == null) {
            return;
        }

        if (mSelectedFolder.isAnimating()) {
            mSelectedFolder.setAnimationCompleteRunnable(closeRunnable);
        } else {
            closeRunnable.run();
        }
    }

    @Override
    public void logActionCommand(int command) {

    }

    protected void animateShowFolderName() {
        mLabel.hideSideTitle(false);
    }

    @Override
    protected boolean isOfType(@FloatingViewType int type) {
        return (type & TYPE_FOLDER) != 0;
    }

    public FolderTabStrip getLabel() {
        return mLabel;
    }

    public FolderViewPager getPager() {
        return mPager;
    }

    public boolean isHovering() {
        return mIsHovering;
    }

    public boolean setHovering(boolean isHovering) {
        if (mIsHovering != isHovering) {
            mIsHovering = isHovering;

            for (Folder folder : mFolders) {
                if (folder != mSelectedFolder) {
                    folder.scrollToTop();
                }
            }
            return true;
        }

        return false;
    }

    public int getCurrentPageIndex() {
        return mPager.getCurrentItem();
    }

    public void snapToPage(int index) {
        mPager.setCurrentItem(index, true);
    }

    public Folder getFolderForIndex(int index) {
        return mFolders.get(index);
    }

    public int getPageCount() {
        return mFolders.size();
    }

    @Override
    public void setInsets(Rect insets) {
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    public void scrollRight() {
        if (mPager.getCurrentItem() < getPageCount()-1) {
            mPager.setCurrentItem(mPager.getCurrentItem()+1, true);
        }
    }

    public void scrollLeft() {
        if (mPager.getCurrentItem() > 0) {
            mPager.setCurrentItem(mPager.getCurrentItem()-1, true);
        }
    }

    public void setDarkenProgress(float progress) {
        getBackground().setColorFilter(Color.argb((int) (125 * progress),
                0, 0, 0), PorterDuff.Mode.DARKEN);
        getBackground().invalidateSelf();
    }

    // Chenyee <CY_Bug> <xiaopeng> <20181025> for CSW1809A-162 begin
    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        DragLayer dl = mLauncher.getDragLayer();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchOutSpace = !dl.isEventOver(mSelectedFolder, ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (dl.isEventOverView(mLabel, ev)) {
                    return false;
                }

                if (mLauncher.isInEditMode()
                        && dl.isEventOver(mLauncher.getEditModePanel(), ev)) {
                    return false;
                }
                if (mTouchOutSpace && !dl.isEventOver(mSelectedFolder, ev)) {
                    close(true);
                    return true;
                }
                break;
        }
        // Chenyee <CY_Bug> <xiaopeng> <20181025> for CSW1809A-162 end
        return false;
    }
}
