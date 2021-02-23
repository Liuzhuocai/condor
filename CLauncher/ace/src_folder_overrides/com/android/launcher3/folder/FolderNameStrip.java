package com.android.launcher3.folder;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.launcher3.R;

import java.lang.ref.WeakReference;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;


/**
 * Created by wangqing on 12/23/15.
 */
public class FolderNameStrip extends ViewGroup {
    private static final String TAG = "FolderTitleStrip";
    private static final int[] ATTRS = new int[]{16842804, 16842901, 16842904, 16842927};
    private static final int[] TEXT_ATTRS = new int[]{16843660};
    private static final int COLOR_MASK = 0xFFFFFF;
    private static final int ALPHA_MASK = 0xFF;
    FolderViewPager mPager;
    TextView mPrevText;
    TextView mCurrText;
    TextView mNextText;
    private int mLastKnownCurrentPage;
    private float mLastKnownPositionOffset;
    private int mScaledTextSpacing;
    private int mGravity;
    private boolean mUpdatingText;
    private boolean mUpdatingPositions;
    private final PageListener mPageListener;
    private WeakReference<PagerAdapter> mWatchingAdapter;
    private int mNonPrimaryAlpha;
    protected boolean isForbid = false;
    int mTextColor;
    private int mCustomTitleSpace ;

    public FolderNameStrip(Context context) {
        this(context, (AttributeSet) null);
    }

    public FolderNameStrip(final Context context, AttributeSet attrs) {
        super(context, attrs);
        mLastKnownCurrentPage = -1;
        mLastKnownPositionOffset = -1.0F;
        mPageListener = new FolderNameStrip.PageListener();
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(mPrevText = new TextView(context),params);
        addView(mCurrText = new TextView(context),params);
        addView(mNextText = new TextView(context),params);
        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        int textAppearance = a.getResourceId(0, 0);

        int defaultColor = Color.WHITE;
        setTextColor(defaultColor);
        setClipChildren(false);

        mCurrText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mPager.isScrolling()) {
                    Log.d(TAG, "can't open FolderTitleEditDialog, mPager is scrolling!");
                    return;
                }

                if (mPager == null) {
                    Log.d(TAG, "can't open FolderTitleEditDialog, mPager is null!");
                    return;
                }

                Folder folder = mPager.getCurrentFolder();
                if (folder == null) {
                    Log.d(TAG, "can't open FolderTitleEditDialog, folder is null!");
                    return;
                }

                folder.startEditingFolderName();
            }
        });

        mCustomTitleSpace =getResources().getDimensionPixelSize(R.dimen.folder_page_name_space);

        mPrevText.setAlpha(0);
        mPrevText.setGravity(Gravity.CENTER);
        mPrevText.setEllipsize(TextUtils.TruncateAt.END);

        mCurrText.setGravity(Gravity.CENTER);
        mCurrText.setScaleX(1.2f);
        mCurrText.setScaleY(1.2f);
        int p = mCurrText.getPaddingTop();
        mCurrText.setPadding(p, 0, 0, 0);
        mCurrText.setEllipsize(TextUtils.TruncateAt.END);
        mCurrText.setMaxEms(6);

        mNextText.setAlpha(0);
        mNextText.setGravity(Gravity.CENTER);
        mNextText.setEllipsize(TextUtils.TruncateAt.END);

        int textSize = a.getDimensionPixelSize(1, 0);
        if(textSize != 0) {
            setTextSize(0, (float) textSize);
        }

        mGravity = a.getInteger(3, 80);
        a.recycle();
        mTextColor = mCurrText.getTextColors().getDefaultColor();
        setNonPrimaryAlpha(0.6F);
        mPrevText.setMaxEms(6);

        mNextText.setMaxEms(6);
        if(textAppearance != 0) {
            TypedArray density = context.obtainStyledAttributes(textAppearance, TEXT_ATTRS);
            density.recycle();
        }

        mPrevText.setSingleLine();
        mCurrText.setSingleLine();
        mNextText.setSingleLine();

        mScaledTextSpacing = 0;
    }

    public void setTextSpacing(int spacingPixels) {
        mScaledTextSpacing = spacingPixels;
        requestLayout();
    }

    public int getTextSpacing() {
        return mScaledTextSpacing;
    }

    public void setNonPrimaryAlpha(float alpha) {
        mNonPrimaryAlpha = (int)(alpha * 255) & ALPHA_MASK;
        int transparentColor = mNonPrimaryAlpha << 24 | mTextColor & COLOR_MASK;
        mPrevText.setTextColor(transparentColor);
        mNextText.setTextColor(transparentColor);
    }

    private void setCenterTextAlpha(float percent){
        int currAlpha = mNonPrimaryAlpha + (int)((255 - mNonPrimaryAlpha) * percent);
        int transparentColor = currAlpha << 24 | mTextColor & COLOR_MASK;
        mCurrText.setTextColor(transparentColor);
    }

    public void setTextColor(int color) {
        mTextColor = color;
        mCurrText.setTextColor(color);
        int transparentColor = mNonPrimaryAlpha << 24 | mTextColor & COLOR_MASK;
        mPrevText.setTextColor(transparentColor);
        mNextText.setTextColor(transparentColor);
    }

    public void setTextSize(int unit, float size) {
        mPrevText.setTextSize(unit, size);
        mCurrText.setTextSize(unit, size);
        mNextText.setTextSize(unit, size);
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
        requestLayout();
    }

    public void setPageChangeListener(FolderViewPager pager){
        mPager = pager;
        PagerAdapter adapter = pager.getAdapter();
        mPager.setOnPageChangeListener(mPageListener);
        updateAdapter(mWatchingAdapter != null?(PagerAdapter)mWatchingAdapter.get():null, adapter);
    }

    void updateText(int currentItem, PagerAdapter adapter) {
        if (adapter.getCount() == 0) {
            return;
        }

        int itemCount = adapter != null?adapter.getCount():0;
        mUpdatingText = true;
        CharSequence text = null;
        if(currentItem >= 1 && adapter != null) {
            text = adapter.getPageTitle(currentItem - 1);
        }

        mPrevText.setText(text);
        mCurrText.setText(adapter != null && currentItem < itemCount?adapter.getPageTitle(currentItem):null);
        text = null;
        if(currentItem + 1 < itemCount && adapter != null) {
            text = adapter.getPageTitle(currentItem + 1);
        }

        mNextText.setText(text);
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int childWidth = (int)((float)(width - 4 * mCustomTitleSpace) / 3);
        int childHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        mPrevText.measure(childWidthSpec, childHeightSpec);
        mCurrText.measure(childWidthSpec, childHeightSpec);
        mNextText.measure(childWidthSpec, childHeightSpec);
        mLastKnownCurrentPage = currentItem;
        if(!mUpdatingPositions) {
            updateTextPositions(currentItem, mLastKnownPositionOffset, false);
        }

        mUpdatingText = false;
    }

    public void requestLayout() {
        if(!mUpdatingText) {
            super.requestLayout();
        }
    }

    void updateAdapter(PagerAdapter oldAdapter, PagerAdapter newAdapter) {
        if(oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(mPageListener);
            mWatchingAdapter = null;
        }

        if(newAdapter != null) {
            newAdapter.registerDataSetObserver(mPageListener);
            mWatchingAdapter = new WeakReference(newAdapter);
        }

        if(mPager != null) {
            mLastKnownCurrentPage = -1;
            mLastKnownPositionOffset = -1.0F;
            updateText(mPager.getCurrentItem(), newAdapter);
            requestLayout();
        }
    }

    void updateTextPositions(int position, float positionOffset, boolean force) {
        if(position != mLastKnownCurrentPage) {
            updateText(position, mPager.getAdapter());
        } else if(!force && positionOffset == mLastKnownPositionOffset) {
            return;
        }

        mUpdatingPositions = true;
        int prevWidth = mPrevText.getMeasuredWidth();
        int currWidth = mCurrText.getMeasuredWidth();
        int nextWidth = mNextText.getMeasuredWidth();
        int halfCurrWidth = currWidth / 2;
        int stripWidth = getWidth();
        int stripHeight = getHeight();
        int paddingLeft = mCustomTitleSpace;
        int paddingRight = mCustomTitleSpace;
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int textPaddedLeft = paddingLeft + halfCurrWidth;
        int textPaddedRight = paddingRight + halfCurrWidth;
        int contentWidth = stripWidth - textPaddedLeft - textPaddedRight;
        float currOffset = positionOffset + 0.5F;
        if(currOffset > 1.0F) {
            --currOffset;
        }

        int currCenter = stripWidth - textPaddedRight - (int)((float)contentWidth * currOffset);
        int currLeft = currCenter - currWidth / 2;
        int currRight = currLeft + currWidth;
        int prevBaseline = mPrevText.getBaseline();
        int currBaseline = mCurrText.getBaseline();
        int nextBaseline = mNextText.getBaseline();
        int maxBaseline = Math.max(Math.max(prevBaseline, currBaseline), nextBaseline);
        int prevTopOffset = maxBaseline - prevBaseline;
        int currTopOffset = maxBaseline - currBaseline;
        int nextTopOffset = maxBaseline - nextBaseline;
        int alignedPrevHeight = prevTopOffset + mPrevText.getMeasuredHeight();
        int alignedCurrHeight = currTopOffset + mCurrText.getMeasuredHeight();
        int alignedNextHeight = nextTopOffset + mNextText.getMeasuredHeight();
        int maxTextHeight = Math.max(Math.max(alignedPrevHeight, alignedCurrHeight), alignedNextHeight);
        int vgrav = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        int currTop;
        int nextTop;
        int prevTop;
        int prevLeft;
        int nextLeft;
        switch(vgrav) {
            case Gravity.CENTER_VERTICAL:
                prevLeft = stripHeight - paddingTop - paddingBottom;
                nextLeft = (prevLeft - maxTextHeight) / 2;
                prevTop = nextLeft + prevTopOffset;
                currTop = nextLeft + currTopOffset;
                nextTop = nextLeft + nextTopOffset;
                break;
            case Gravity.BOTTOM:
                int bottomGravTop = stripHeight - paddingBottom - maxTextHeight;
                prevTop = bottomGravTop + prevTopOffset;
                currTop = bottomGravTop + currTopOffset;
                nextTop = bottomGravTop + nextTopOffset;
                break;
            case Gravity.TOP:
            default:
                prevTop = paddingTop + prevTopOffset;
                currTop = paddingTop + currTopOffset;
                nextTop = paddingTop + nextTopOffset;
                break;
        }

        float percent = 0;
        float scaleCenter = 0;
        if(currOffset > 0.5){
            percent = 1 - currOffset;
            scaleCenter = 1+(float)((percent)*0.2/0.5);
        }else{
            percent = currOffset;
            scaleCenter = 1+(float)(percent*0.2/0.5);
        }

        mCurrText.setScaleX(scaleCenter);
        mCurrText.setScaleY(scaleCenter);
        mCurrText.setGravity(Gravity.CENTER_VERTICAL);
        setCenterTextAlpha(percent * 2);

        mCurrText.layout(currLeft, currTop, currRight, currTop + mCurrText.getMeasuredHeight());

        prevLeft = paddingLeft;
        prevWidth = Math.min(prevWidth, currLeft - paddingRight - prevLeft);
        mPrevText.layout(prevLeft, prevTop, prevLeft + prevWidth, prevTop + mPrevText.getMeasuredHeight());

        int nextRight = stripWidth - paddingRight;
        nextLeft = Math.max(nextRight - nextWidth, currRight + paddingLeft);
        mNextText.layout(nextLeft, nextTop, nextRight, nextTop + mNextText.getMeasuredHeight());
        mLastKnownPositionOffset = positionOffset;
        mUpdatingPositions = false;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if(widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Must measure with an exact width");
        }

        int minHeight = getMinHeight();
        int padding1 = getPaddingTop() + getPaddingBottom();
        int childWidth = (int)((float)(widthSize - 4 * mCustomTitleSpace) / 3);
        int childHeight = heightSize - padding1;
        int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        mPrevText.measure(childWidthSpec, childHeightSpec);
        mCurrText.measure(childWidthSpec, childHeightSpec);
        mNextText.measure(childWidthSpec, childHeightSpec);
        if(heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize);
        } else {
            int textHeight = mCurrText.getMeasuredHeight();
            setMeasuredDimension(widthSize, Math.max(minHeight, textHeight + padding1));
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(mPager != null) {
            float offset = mLastKnownPositionOffset >= 0.0F?mLastKnownPositionOffset:0.0F;
            updateTextPositions(mLastKnownCurrentPage, offset, true);
        }

    }

    public void updatePageTitle(int position) {
        if (mPager != null) {
            updateText(mPager.getCurrentItem(), mPager.getAdapter());
            updateTextPositions(position, 0, true);
        }
    }

    int getMinHeight() {
        int minHeight = 0;
        Drawable bg = getBackground();
        if(bg != null) {
            minHeight = bg.getIntrinsicHeight();
        }

        return minHeight;
    }

    public class PageListener extends DataSetObserver implements ViewPager.OnPageChangeListener {
        private int mScrollState;

        private PageListener() {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if(positionOffset > 0.5F) {
                ++position;
            }

            updateTextPositions(position, positionOffset, false);
        }

        public void onPageSelected(int position) {
            if(mScrollState == 0) {
                updateText(mPager.getCurrentItem(), mPager.getAdapter());
                float offset = mLastKnownPositionOffset >= 0.0F? mLastKnownPositionOffset:0.0F;
                updateTextPositions(mPager.getCurrentItem(), offset, true);
            }

        }

        public void onPageScrollStateChanged(int state) {
            mScrollState = state;

            if(mPager == null){
                return;
            }

            Folder folder = mPager.getCurrentFolder();

            if (folder == null) {
                return;
            }

            if (mScrollState == ViewPager.SCROLL_STATE_SETTLING && folder != null) {
                mPager.setScrollState(false);
                folder.requestFocus();
                return;
            }

            if(mScrollState == ViewPager.SCROLL_STATE_IDLE){
                mPager.setScrollState(false);
            }

            if (mScrollState == ViewPager.SCROLL_STATE_DRAGGING) {
                mPager.setScrollState(true);
            }


        }

        public void onChanged() {
            updateText(mPager.getCurrentItem(), mPager.getAdapter());
            float offset = mLastKnownPositionOffset >= 0.0F? mLastKnownPositionOffset:0.0F;
            updateTextPositions(mPager.getCurrentItem(), offset, true);
        }
    }

    public void hideSideTitle(boolean isHide) {
        if (isHide) {
            PropertyValuesHolder alphaholder = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
            ObjectAnimator.ofPropertyValuesHolder(mPrevText, alphaholder).setDuration(500).start();

            ObjectAnimator.ofPropertyValuesHolder(mNextText, alphaholder).setDuration(500).start();
        } else {
            PropertyValuesHolder alphaholder = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
            ObjectAnimator.ofPropertyValuesHolder(mPrevText, alphaholder).setDuration(500).start();

            ObjectAnimator.ofPropertyValuesHolder(mNextText, alphaholder).setDuration(500).start();
        }
    }
}
