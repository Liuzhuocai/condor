package com.condor.launcher.unreadnotifier;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.folder.FolderIcon;

/**
 * Created by Bruce on 2019/1/30.
 */
public class BadgeUtils {
    private static final float ROUNDRECT_ARC_X = 30.0f;
    private static final float ROUNDRECT_ARC_Y = 30.0f;

    private static final String BLANK_STRING = " ";

    private static Paint sBgPaint;
    private static Paint sTextPaint;

    public enum BadgeMode {LT_BADGE, RT_BADGE, LB_BADGE, RB_BADGE}

    public static void drawBadge(Canvas canvas, View v, String text) {
        drawBadge(canvas, v, text, BadgeMode.RT_BADGE);
    }

    public static Rect drawBadge(Canvas canvas, View v, String text, BadgeMode mode) {
        if (canvas == null || v == null) {
            return null;
        }

        //init text
        if (TextUtils.isEmpty(text)) {
            text = BLANK_STRING;
        }

        //init view rect
        Rect vRect = new Rect();
        v.getDrawingRect(vRect);

        //init icon rect
        Rect iconRect = new Rect(vRect);
        if (v instanceof BubbleTextView) {
            iconRect = ((BubbleTextView)v).getIconRect();
        } else if (v instanceof FolderIcon) {
            iconRect = ((FolderIcon)v).getIconRect();
        }

        //init paint
        Paint textPaint = sTextPaint == null ? createTextPaint(v.getContext()) : sTextPaint;
        Paint bgPaint = sBgPaint == null ? createBgPaint(v.getContext()) : sBgPaint;

        canvas.save();

        //init text bounds
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        Rect textBounds = new Rect();
        textBounds.left = 0;
        textBounds.top = 0;
        textBounds.right = Math.round(textPaint.measureText(text));
        textBounds.bottom = Math.round(fm.descent - fm.ascent);
        long minHeight = Math.round(Math.sqrt((textBounds.height() * textBounds.height()) << 1));
        if (textBounds.width() <= textBounds.height()) {
            textBounds.right = textBounds.bottom = (int) minHeight;
        } else {
            textBounds.right += Math.round(textPaint.measureText(BLANK_STRING));
            textBounds.bottom = (int) minHeight;
        }

        //draw badge bg
        Rect badgeRect = getBadgeRect(vRect, iconRect, textBounds, mode);
        if (badgeRect.width() == badgeRect.height()) {
            canvas.drawCircle(badgeRect.centerX(), badgeRect.centerY(), (badgeRect.width() >> 1), bgPaint);
        } else {
            canvas.drawRoundRect(new RectF(badgeRect), ROUNDRECT_ARC_X, ROUNDRECT_ARC_Y, bgPaint);
        }

        //draw badge text
        Point drawPoint = UnreadUtils.getTextDrawPoint(badgeRect, fm);
        canvas.drawText(text, drawPoint.x, drawPoint.y, textPaint);

        canvas.restore();
        return badgeRect;
    }

    private static Paint createBgPaint(Context context) {
        Resources res = context.getResources();
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(res.getColor(R.color.badge_bg_color));
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);
        return bgPaint;
    }

    private static Paint createTextPaint(Context context) {
        Resources res = context.getResources();
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(res.getColor(R.color.badge_text_color));
        textPaint.setTextSize(res.getDimension( R.dimen.badge_text_size));
        Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
        textPaint.setTypeface(font);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        return textPaint;
    }

    private static Rect getBadgeRect(Rect viewRect, Rect iconRect, Rect textBounds, BadgeMode mode) {
        Rect badgeRect = new Rect(textBounds);
        int halfWidth;
        int halfHeight;

        switch (mode) {
            case LT_BADGE:
                textBounds.offsetTo(iconRect.left, iconRect.top);
                textBounds.right = Math.min(textBounds.right, iconRect.left + (iconRect.width() >> 1));
                halfWidth = textBounds.width() >> 1;
                halfHeight = textBounds.height() >> 1;

                badgeRect.left = iconRect.left - halfWidth;
                badgeRect.top = iconRect.top - halfHeight;
                badgeRect.right = iconRect.left + halfWidth;
                badgeRect.bottom = iconRect.top + halfHeight;
                badgeRect.offset(halfWidth, halfHeight);
                break;
            case RT_BADGE:
                textBounds.offsetTo(iconRect.right - (iconRect.width() >> 1), iconRect.top);
                textBounds.right = Math.min(textBounds.right, iconRect.right);
                halfWidth = textBounds.width() >> 1;
                halfHeight = textBounds.height() >> 1;

                badgeRect.left = iconRect.right - halfWidth;
                badgeRect.top = iconRect.top - halfHeight;
                badgeRect.right = iconRect.right + halfWidth;
                badgeRect.bottom = iconRect.top + halfHeight;
                badgeRect.offset(-halfWidth, halfHeight);
                break;
            case LB_BADGE:
                textBounds.offsetTo(iconRect.left, iconRect.top);
                textBounds.right = Math.min(textBounds.right, iconRect.left + (iconRect.width() >> 1));
                halfWidth = textBounds.width() >> 1;
                halfHeight = textBounds.height() >> 1;

                badgeRect.left = iconRect.left - halfWidth;
                badgeRect.top = iconRect.bottom - halfHeight;
                badgeRect.right = iconRect.left + halfWidth;
                badgeRect.bottom = iconRect.bottom + halfHeight;
                badgeRect.offset(halfWidth, -halfHeight);
                break;
            case RB_BADGE:
                textBounds.offsetTo(iconRect.right - (iconRect.width() >> 1), iconRect.top);
                textBounds.right = Math.min(textBounds.right, iconRect.right);
                halfWidth = textBounds.width() >> 1;
                halfHeight = textBounds.height() >> 1;

                badgeRect.left = iconRect.right - halfWidth;
                badgeRect.top = iconRect.bottom - halfHeight;
                badgeRect.right = iconRect.right + halfWidth;
                badgeRect.bottom = iconRect.bottom + halfHeight;
                badgeRect.offset(-halfWidth, -halfHeight);
                break;
        }

        badgeRect.right = badgeRect.height() > badgeRect.width() ?
                badgeRect.left + badgeRect.height() : badgeRect.right;
        Point offset = calcOffset(badgeRect, viewRect, mode);
        badgeRect.offset(offset.x, offset.y);

        return badgeRect;
    }

    private static Point calcOffset(Rect badgeRect, Rect viewRect, BadgeMode mode) {
        Point p = new Point();
        switch (mode) {
            case LT_BADGE:
                p.x = badgeRect.left < viewRect.left ? viewRect.left - badgeRect.left + 1 : 0;
                p.y = badgeRect.top < viewRect.top ? viewRect.top - badgeRect.top + 1 : 0;
                break;
            case RT_BADGE:
                p.x = badgeRect.right > viewRect.right ? viewRect.right - badgeRect.right - 1 : 0;
                p.y = badgeRect.top < viewRect.top ? viewRect.top - badgeRect.top + 1 : 0;
                break;
            case LB_BADGE:
                p.x = badgeRect.left < viewRect.left ? viewRect.left - badgeRect.left + 1 : 0;
                p.y = badgeRect.bottom > viewRect.bottom ? viewRect.bottom - badgeRect.bottom - 1 : 0;
                break;
            case RB_BADGE:
                p.x = badgeRect.right > viewRect.right ? viewRect.right - badgeRect.right - 1 : 0;
                p.y = badgeRect.bottom > viewRect.bottom ? viewRect.bottom - badgeRect.bottom - 1 : 0;
                break;
        }

        return p;
    }

    public static void setBgPaint(final Paint paint) {
        sBgPaint = paint;
    }

    public static void setTextPaint(final Paint paint) {
        sTextPaint = paint;
    }
}
