package com.condor.launcher.theme.bean;

import java.io.Serializable;

/**
 * author：liuzuo on 18-12-28 12:13
 */
public class ThemeConfigBean implements Serializable {
    private float coordinate_x;
    private float coordinate_y;
    private int count;
    private float padding_left;
    private float padding_right;
    private float padding_top;
    private float padding_bottom;
    private float icon_size;
    private float folder_bg_size;

    private float scale;

    public float getCoordinate_x() {
        return coordinate_x;
    }

    public void setCoordinate_x(float coordinate_x) {
        this.coordinate_x = coordinate_x;
    }

    public float getCoordinate_y() {
        return coordinate_y;
    }

    public void setCoordinate_y(float coordinate_y) {
        this.coordinate_y = coordinate_y;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public float getPadding_left() {
        return padding_left;
    }

    public void setPadding_left(float padding_left) {
        this.padding_left = padding_left;
    }

    public float getPadding_right() {
        return padding_right;
    }

    public void setPadding_right(float padding_right) {
        this.padding_right = padding_right;
    }

    public float getPadding_top() {
        return padding_top;
    }

    public void setPadding_top(float padding_top) {
        this.padding_top = padding_top;
    }

    public float getPadding_bottom() {
        return padding_bottom;
    }

    public void setPadding_bottom(float padding_bottom) {
        this.padding_bottom = padding_bottom;
    }

    @Override
    public String toString() {
        return "ThemeConfigBean{" +
                "coordinate_x=" + coordinate_x +
                ", coordinate_y=" + coordinate_y +
                ", count=" + count +
                ", padding_left=" + padding_left +
                ", padding_right=" + padding_right +
                ", padding_top=" + padding_top +
                ", padding_bottom=" + padding_bottom +
                ", icon_size=" + icon_size +
                ", scale=" + scale +
                '}';
    }

    public float getIcon_size() {
        return icon_size;
    }

    public void setIcon_size(float icon_size) {
        this.icon_size = icon_size;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
    public float getFolder_bg_size() {
        return folder_bg_size;
    }

    public void setFolder_bg_size(float folder_bg_size) {
        this.folder_bg_size = folder_bg_size;
    }
}
