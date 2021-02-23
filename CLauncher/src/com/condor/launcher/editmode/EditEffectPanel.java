package com.condor.launcher.editmode;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.condor.launcher.switcher.effect.Effect;
import com.condor.launcher.switcher.effect.EffectManager;
import com.condor.launcher.views.PanelPagedView;

import static com.condor.launcher.switcher.Switcher.EFFECT_SWITCHER;

/**
 * Created by Perry on 19-1-21
 */
public class EditEffectPanel extends PanelPagedView {
    private static final int SPAN = 4;
    private final Launcher mLauncher;
    private int mItemIndex = 0;

    public EditEffectPanel(@NonNull Context context) {
        this(context, null);
    }

    public EditEffectPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditEffectPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        EffectManager manager = EffectManager.getInstance();
        manager.getEffects().forEach(this::addEffectItem);
    }

    private void addEffectItem(Effect effect) {
        LinearLayout itemLayout;
        if (mItemIndex++ % SPAN == 0) {
            itemLayout = (LinearLayout) LayoutInflater.from(getContext()).
                    inflate(R.layout.effect_item_layout, this, false);
            addView(itemLayout);
        } else {
            itemLayout = (LinearLayout) getPageAt(getChildCount()-1);
        }
        TextView item = (TextView) LayoutInflater.from(getContext()).
                inflate(R.layout.effect_item, itemLayout, false);
        item.setCompoundDrawablesWithIntrinsicBounds(0, effect.getIcon(), 0, 0);
        item.setText(effect.getTitle());
        // Perry: Add selected state for effect item: start
        item.setOnClickListener(v-> {
            setSelected(item);
            EFFECT_SWITCHER.doSwitch(getContext(), effect.value);
        });
        item.setSelected(effect == EFFECT_SWITCHER.get());
        // Perry: Add selected state for effect item: end
        itemLayout.addView(item);
    }

    // Perry: Add selected state for effect item: start
    private void setSelected(TextView view) {
        for (int i = 0; i < getChildCount(); i++) {
            LinearLayout container = (LinearLayout)getChildAt(i);
            for (int j = 0; j < container.getChildCount(); j++) {
                View child = container.getChildAt(j);
                child.setSelected(child == view);
            }
        }
    }
    // Perry: Add selected state for effect item: end

    // Perry: Snap to target page when jump to effect: start
    private int getSelectedPageIndex() {
        for (int i = 0; i < getChildCount(); i++) {
            LinearLayout container = (LinearLayout)getChildAt(i);
            for (int j = 0; j < container.getChildCount(); j++) {
                View child = container.getChildAt(j);
                if (child.isSelected()) {
                    return i;
                }
            }
        }
        return 0;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            int pageIndex = getSelectedPageIndex();
            if (pageIndex != 0) {
                postDelayed(() -> {
                    if (getNextPage() == 0) {
                        snapToPage(pageIndex);
                    }
                }, 300);
            }
        }
    }
    // Perry: Snap to target page when jump to effect: end
}
