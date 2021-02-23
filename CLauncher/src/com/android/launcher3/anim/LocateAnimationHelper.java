package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.Utilities;
import com.android.launcher3.folder.FolderIcon;


/**
 * Created by Perry on 19-1-15
 */
public class LocateAnimationHelper {
    public static void startLocateAnimation(View view) {
        AnimatorSet anim = getLocateAnimation(view);
        if (anim != null) {
            anim.start();
        }
    }

    public static void startLocateAnimation(final View view, final Runnable onCompleteRunnable) {
        AnimatorSet anim = getLocateAnimation(view, onCompleteRunnable);
        if (anim != null) {
            anim.start();
        }
    }

    public static AnimatorSet getLocateAnimation(View view) {
        return getLocateAnimation(view, null);
    }

    public static AnimatorSet getLocateAnimation(final View view, final Runnable onCompleteRunnable) {
        if (!(view instanceof BubbleTextView
                || view instanceof FolderIcon)) {
            return null;
        }

        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        PropertyValuesHolder scaleToX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.3f);
        PropertyValuesHolder scaleToY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.3f);
        Animator scaleTo = LauncherAnimUtils.ofPropertyValuesHolder(view, scaleToX,
                scaleToY);
        scaleTo.setDuration(300);

        PropertyValuesHolder scaleFromX = PropertyValuesHolder.ofFloat("scaleX", 1.3f, 1.0f);
        PropertyValuesHolder scaleFromY = PropertyValuesHolder.ofFloat("scaleY", 1.3f, 1.0f);
        Animator scaleFrom = LauncherAnimUtils.ofPropertyValuesHolder(view, scaleFromX,
                scaleFromY);
        scaleFrom.setDuration(300);

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        ObjectAnimator nop = nope(view);

        anim.play(scaleTo).before(nop).before(scaleFrom);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        });

        return anim;
    }

    public static ObjectAnimator nope(View view) {
        int delta = Utilities.pxFromDp(8, view.getResources().getDisplayMetrics());

        PropertyValuesHolder pvhTranslateX = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                Keyframe.ofFloat(0f, 0),
                Keyframe.ofFloat(.10f, -delta),
                Keyframe.ofFloat(.26f, delta),
                Keyframe.ofFloat(.42f, -delta),
                Keyframe.ofFloat(.58f, delta),
                Keyframe.ofFloat(.74f, -delta),
                Keyframe.ofFloat(.90f, delta),
                Keyframe.ofFloat(1f, 0f)
        );

        return ObjectAnimator.ofPropertyValuesHolder(view, pvhTranslateX).
                setDuration(500);
    }
}
