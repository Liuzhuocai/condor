package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EdgeEffect;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import static androidx.dynamicanimation.animation.SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY;
import static androidx.dynamicanimation.animation.SpringForce.STIFFNESS_LOW;
import static androidx.dynamicanimation.animation.SpringForce.STIFFNESS_MEDIUM;


public class SpringScrollView extends OverScrollView {
    public static final float STIFFNESS = (STIFFNESS_MEDIUM + STIFFNESS_LOW) / 2;
    public static final float DAMPING_RATIO = DAMPING_RATIO_MEDIUM_BOUNCY;
    public static final float VELOCITY_MULTIPLIER = 0.3f;

    private static final FloatPropertyCompat<SpringScrollView> DAMPED_SCROLL =
            new FloatPropertyCompat<SpringScrollView>("value") {

                @Override
                public float getValue(SpringScrollView object) {
                    return object.mDampedScrollShift;
                }

                @Override
                public void setValue(SpringScrollView object, float value) {
                    object.setDampedScrollShift(value);
                }
            };

    private final SpringAnimation mSpring;

    private float mDampedScrollShift = 0;
    private SpringEdgeEffect mActiveEdge;

    public SpringScrollView(Context context) {
        this(context, null);
    }

    public SpringScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpringScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSpring = new SpringAnimation(this, DAMPED_SCROLL, 0);
        mSpring.setSpring(new SpringForce(0)
                .setStiffness(STIFFNESS)
                .setDampingRatio(DAMPING_RATIO));
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (mDampedScrollShift != 0) {
            canvas.translate(0, mDampedScrollShift);
            boolean result = super.drawChild(canvas, child, drawingTime);
            canvas.translate(0, -mDampedScrollShift);
            return result;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private void setActiveEdge(SpringEdgeEffect edge) {
        if (mActiveEdge != edge && mActiveEdge != null) {
            mActiveEdge.mDistance = 0;
        }
        mActiveEdge = edge;
    }

    protected void setDampedScrollShift(float shift) {
        if (shift != mDampedScrollShift) {
            mDampedScrollShift = shift;
            invalidate();
        }
    }

    private void finishScrollWithVelocity(float velocity) {
        mSpring.setStartVelocity(velocity);
        mSpring.setStartValue(mDampedScrollShift);
        mSpring.start();
    }

    @Override
    protected EdgeEffect createTopEdgeEffect() {
        return new SpringEdgeEffect(getContext(), +VELOCITY_MULTIPLIER);
    }

    @Override
    protected EdgeEffect createBottomEdgeEffect() {
        return new SpringEdgeEffect(getContext(), -VELOCITY_MULTIPLIER);
    }

    protected void finishWithShiftAndVelocity(float shift, float velocity,
                                              DynamicAnimation.OnAnimationEndListener listener) {
        setDampedScrollShift(shift);
        mSpring.addEndListener(listener);
        finishScrollWithVelocity(velocity);
    }

    protected class SpringEdgeEffect extends EdgeEffect {

        private final float mVelocityMultiplier;

        private float mDistance;

        public SpringEdgeEffect(Context context, float velocityMultiplier) {
            super(context);
            mVelocityMultiplier = velocityMultiplier;
        }

        @Override
        public boolean draw(Canvas canvas) {
            return false;
        }

        @Override
        public void onAbsorb(int velocity) {
            finishScrollWithVelocity(velocity * mVelocityMultiplier);
        }

        @Override
        public void onPull(float deltaDistance, float displacement) {
            setActiveEdge(this);
            mDistance += Math.abs(deltaDistance) * (mVelocityMultiplier / 3f);
            setDampedScrollShift(mDistance * getHeight());
        }

        @Override
        public void onRelease() {
            mDistance = 0;
            finishScrollWithVelocity(0);
        }
    }
}
