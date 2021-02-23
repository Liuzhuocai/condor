package com.condor.launcher.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.launcher3.R;
import com.condor.launcher.graphics.SelectedDrawable;

import java.util.List;

/**
 * Created by Perry on 19-1-25
 */
public class ListPreferenceAdapter extends RecyclerView.Adapter<ListPreferenceAdapter.ViewHolder>{
    private final List<CharSequence> mData;
    private OnItemClickListener itemClickListener = null;
    private int mSelectedPos = -1;

    public ListPreferenceAdapter(List<CharSequence> list, int select) {
        mData = list;
        mSelectedPos = select;
    }

    @Override
    public ListPreferenceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.condor_list_preference_item, parent, false);
        return new ListPreferenceAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListPreferenceAdapter.ViewHolder holder, final int position) {
        holder.mText.setText(mData.get(position));
        if (position == mSelectedPos) {
            holder.mText.setTextColor(Color.parseColor("#35B4FF"));
        } else {
            holder.mText.setTextColor(Color.parseColor("#333333"));
        }
        Drawable select = holder.mText.getCompoundDrawables()[0];
        if (select instanceof SelectedDrawable) {
            ((SelectedDrawable)select).setSelected(position == mSelectedPos);
        }

        holder.itemView.setOnClickListener(v-> {
            mSelectedPos = position;
            itemClickListener.onItemClick(position);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mText;

        ViewHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.condor_list_text);
            mText.setCompoundDrawablesWithIntrinsicBounds(new SelectedDrawable(mText.getContext()),
                    null, null, null);
        }

    }

}

