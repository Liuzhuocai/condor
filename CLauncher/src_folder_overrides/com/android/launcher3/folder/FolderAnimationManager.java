/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LogDecelerateInterpolator;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.anim.AnimationLayerSet;
import com.android.launcher3.anim.CircleRevealOutlineProvider;

import java.util.List;

import static com.android.launcher3.LauncherAnimUtils.SCALE_PROPERTY;

/**
 * Manages the opening and closing animations for a {@link Folder}.
 *
 * All of the animations are done in the Folder.
 * ie. When the user taps on the FolderIcon, we immediately hide the FolderIcon and show the Folder
 * in its place before starting the animation.
 */
public class FolderAnimationManager {

    private Folder mFolder;
    private FolderPagedView mContent;
    private GradientDrawable mFolderBackground;

    private FolderIcon mFolderIcon;
    private PreviewBackground mPreviewBackground;

    private Context mContext;
    private Launcher mLauncher;

    private final boolean mIsOpening;

    private final int mDuration;
    private final int mDelay;

    private final TimeInterpolator mFolderInterpolator;
    private final TimeInterpolator mLargeFolderPreviewItemOpenInterpolator;
    private final TimeInterpolator mLargeFolderPreviewItemCloseInterpolator;

    private final PreviewItemDrawingParams mTmpParams = new PreviewItemDrawingParams(0, 0, 0, 0);


    public FolderAnimationManager(Folder folder, boolean isOpening) {
        mFolder = folder;
        mContent = folder.mContent;
        mFolderBackground = (GradientDrawable) mFolder.getBackground();

        mFolderIcon = folder.mFolderIcon;
        mPreviewBackground = mFolderIcon.mBackground;

        mContext = folder.getContext();
        mLauncher = folder.mLauncher;

        mIsOpening = isOpening;

        Resources res = mContent.getResources();
        mDuration = res.getInteger(R.integer.config_materialFolderExpandDuration);
        mDelay = res.getInteger(R.integer.config_folderDelay);

        mFolderInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.folder_interpolator);
        mLargeFolderPreviewItemOpenInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.large_folder_preview_item_open_interpolator);
        mLargeFolderPreviewItemCloseInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.large_folder_preview_item_close_interpolator);
    }


    /**
     * Prepares the Folder for animating between open / closed states.
     */
    public AnimatorSet getAnimator() {
        //liuzuo:change folder UI:begin
        if(mIsOpening){
            return getOpeningAnimator() ;
        }else {
            return getClosingAnimator();
        }
        //liuzuo:change folder UI:end
    }

    /**
     * Animate the items on the current page.
     */
    private void addPreviewItemAnimators(AnimatorSet animatorSet, final float folderScale,
            int previewItemOffsetX, int previewItemOffsetY) {
        FolderIcon.PreviewLayoutRule rule = mFolderIcon.getLayoutRule();
        boolean isOnFirstPage = mFolder.mContent.getCurrentPage() == 0;
        final List<BubbleTextView> itemsInPreview = isOnFirstPage
                ? mFolderIcon.getPreviewItems()
                : mFolderIcon.getPreviewItemsOnPage(mFolder.mContent.getCurrentPage());
        final int numItemsInPreview = itemsInPreview.size();
        final int numItemsInFirstPagePreview = isOnFirstPage
                ? numItemsInPreview : FolderIcon.NUM_ITEMS_IN_PREVIEW;

        TimeInterpolator previewItemInterpolator = getPreviewItemInterpolator();

        ShortcutAndWidgetContainer cwc = mContent.getPageAt(0).getShortcutsAndWidgets();
        for (int i = 0; i < numItemsInPreview; ++i) {
            final BubbleTextView btv = itemsInPreview.get(i);
            CellLayout.LayoutParams btvLp = (CellLayout.LayoutParams) btv.getLayoutParams();

            // Calculate the final values in the LayoutParams.
            btvLp.isLockedToGrid = true;
            cwc.setupLp(btv);

            // Match scale of icons in the preview of the items on the first page.
            float previewScale = rule.scaleForItem(0,numItemsInFirstPagePreview);
            float previewSize = rule.getIconSize() * previewScale;
            float iconScale = previewSize / itemsInPreview.get(i).getIconSize();

            final float initialScale = iconScale / folderScale;
            final float finalScale = 1f;
            float scale = mIsOpening ? initialScale : finalScale;
            btv.setScaleX(scale);
            btv.setScaleY(scale);

            // Match positions of the icons in the folder with their positions in the preview
            rule.computePreviewItemDrawingParams(i, numItemsInFirstPagePreview, mTmpParams);
            // The PreviewLayoutRule assumes that the icon size takes up the entire width so we
            // offset by the actual size.
            int iconOffsetX = (int) ((btvLp.width - btv.getIconSize()) * iconScale) / 2;

            final int previewPosX =
                    (int) ((mTmpParams.transX - iconOffsetX + previewItemOffsetX) / folderScale);
            final int previewPosY = (int) ((mTmpParams.transY + previewItemOffsetY) / folderScale);

            final float xDistance = previewPosX - btvLp.x;
            final float yDistance = previewPosY - btvLp.y;

            Animator translationX = getAnimator(btv, View.TRANSLATION_X, xDistance, 0f);
            translationX.setInterpolator(previewItemInterpolator);
            play(animatorSet, translationX);

            Animator translationY = getAnimator(btv, View.TRANSLATION_Y, yDistance, 0f);
            translationY.setInterpolator(previewItemInterpolator);
            play(animatorSet, translationY);

            Animator scaleAnimator = getAnimator(btv, SCALE_PROPERTY, initialScale, finalScale);
            scaleAnimator.setInterpolator(previewItemInterpolator);
            play(animatorSet, scaleAnimator);

            if (mFolder.getItemCount() > FolderIcon.NUM_ITEMS_IN_PREVIEW) {
                // These delays allows the preview items to move as part of the Folder's motion,
                // and its only necessary for large folders because of differing interpolators.
                int delay = mIsOpening ? mDelay : mDelay * 2;
                if (mIsOpening) {
                    translationX.setStartDelay(delay);
                    translationY.setStartDelay(delay);
                    scaleAnimator.setStartDelay(delay);
                }
                translationX.setDuration(translationX.getDuration() - delay);
                translationY.setDuration(translationY.getDuration() - delay);
                scaleAnimator.setDuration(scaleAnimator.getDuration() - delay);
            }

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    // Necessary to initialize values here because of the start delay.
                    if (mIsOpening) {
                        btv.setTranslationX(xDistance);
                        btv.setTranslationY(yDistance);
                        btv.setScaleX(initialScale);
                        btv.setScaleY(initialScale);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    btv.setTranslationX(0.0f);
                    btv.setTranslationY(0.0f);
                    btv.setScaleX(1f);
                    btv.setScaleY(1f);
                }
            });
        }
    }

    private void play(AnimatorSet as, Animator a) {
        play(as, a, a.getStartDelay(), mDuration);
    }

    private void play(AnimatorSet as, Animator a, long startDelay, int duration) {
        a.setStartDelay(startDelay);
        a.setDuration(duration);
        as.play(a);
    }

    private TimeInterpolator getPreviewItemInterpolator() {
        if (mFolder.getItemCount() > FolderIcon.NUM_ITEMS_IN_PREVIEW) {
            // With larger folders, we want the preview items to reach their final positions faster
            // (when opening) and later (when closing) so that they appear aligned with the rest of
            // the folder items when they are both visible.
            return mIsOpening
                    ? mLargeFolderPreviewItemOpenInterpolator
                    : mLargeFolderPreviewItemCloseInterpolator;
        }
        return mFolderInterpolator;
    }

    private Animator getAnimator(View view, Property property, float v1, float v2) {
        return mIsOpening
                ? ObjectAnimator.ofFloat(view, property, v1, v2)
                : ObjectAnimator.ofFloat(view, property, v2, v1);
    }

    private Animator getAnimator(GradientDrawable drawable, String property, int v1, int v2) {
        return mIsOpening
                ? ObjectAnimator.ofArgb(drawable, property, v1, v2)
                : ObjectAnimator.ofArgb(drawable, property, v2, v1);
    }


    //liuzuo:change folder UI:begin
    private AnimatorSet getOpeningAnimator() {
        mFolder.prepareReveal();
        //mFolderIcon.growAndFadeOut();

        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();

        int width = mFolder.getFolderWidth();
        int height = mFolder.getFolderHeight();
        Log.d("liuzuo99","0getPivotX()="+mFolder.getPivotX());
        float transX = - 0.075f * (width / 2 - mFolder.getPivotX());
        float transY = - 0.075f * (height / 2 - mFolder.getPivotY());
        mFolder.setTranslationX(transX);
        mFolder.setTranslationY(transY);
        PropertyValuesHolder tx = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, transX, 0);
        PropertyValuesHolder ty = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, transY, 0);

        Animator drift = ObjectAnimator.ofPropertyValuesHolder(mFolder, tx, ty);
        drift.setDuration(mDuration);
        drift.setStartDelay(mDelay);
        drift.setInterpolator(new LogDecelerateInterpolator(100, 0));

        int rx = (int) Math.max(Math.max(width - mFolder.getPivotX(), 0), mFolder.getPivotX());
        int ry = (int) Math.max(Math.max(height - mFolder.getPivotY(), 0), mFolder.getPivotY());
        float radius = (float) Math.hypot(rx, ry);

        Animator reveal = new CircleRevealOutlineProvider((int) mFolder.getPivotX(),
                (int) mFolder.getPivotY(), 0, radius).createRevealAnimator(mFolder,false);
        reveal.setDuration(mDuration);
        reveal.setInterpolator(new LogDecelerateInterpolator(100, 0));

        mContent.setAlpha(0f);
        Animator iconsAlpha = ObjectAnimator.ofFloat(mContent, "alpha", 0f, 1f);
        iconsAlpha.setDuration(mDuration);
        iconsAlpha.setStartDelay(mDelay);
        iconsAlpha.setInterpolator(new AccelerateInterpolator(1.5f));

        mFolder.mFooter.setAlpha(0f);
        Animator textAlpha = ObjectAnimator.ofFloat(mFolder.mFooter, "alpha", 0f, 1f);
        textAlpha.setDuration(mDuration);
        textAlpha.setStartDelay(mDelay);
        textAlpha.setInterpolator(new AccelerateInterpolator(1.5f));

        anim.play(drift);
        anim.play(iconsAlpha);
        anim.play(textAlpha);
        anim.play(reveal);

        AnimationLayerSet layerSet = new AnimationLayerSet();
        layerSet.addView(mContent);
        layerSet.addView(mFolder.mFooter);
        anim.addListener(layerSet);

        return anim;
    }
    private AnimatorSet getClosingAnimator() {
        AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
        animatorSet.play(LauncherAnimUtils.ofViewAlphaAndScale(mFolder, 0, 0.9f, 0.9f));

        AnimationLayerSet layerSet = new AnimationLayerSet();
        layerSet.addView(mFolder);
        animatorSet.addListener(layerSet);
        animatorSet.setDuration(mDuration);
        return animatorSet;
    }
    //liuzuo:change folder UI:end
}
