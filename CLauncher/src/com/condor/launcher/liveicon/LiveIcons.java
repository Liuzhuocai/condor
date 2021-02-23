package com.condor.launcher.liveicon;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;

import com.condor.launcher.util.FontCache;
import com.condor.launcher.util.JsonLoader;
import com.condor.launcher.util.Logger;
import com.condor.launcher.util.Utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by Perry on 19-2-18
 */
public class LiveIcons {
    private static final String TAG = "LiveIcons";
    private static final String LIVE_ICONS_FILE = "live_icons.json";
    private static final HashMap<String, Class<? extends BaseLiveIcon>> CONFIGS = new HashMap<>();

    static {
        CONFIGS.put("calendar", CalendarLiveIcon.class);
        CONFIGS.put("clockText", ClockLiveIcon.class);
        CONFIGS.put("clockDial", ClockDialIcon.class);
    }

    private int version;
    private Set<String> liveIcons;
    private Calendar calendar;
    private ClockText clockText;
    private ClockDial clockDial;

    public static LiveIcons load(Context context) {
        return JsonLoader.obtain().load(context, LIVE_ICONS_FILE, LiveIcons.class);
    }

    public BaseLiveIcon newLiveIcon(Context context, String name) {
        Class<? extends BaseLiveIcon> aClass = CONFIGS.get(name);
        if (aClass == null) {
            return null;
        }
        try {
            Field f = getClass().getDeclaredField(name);
            f.setAccessible(true);

            BaseConfig config = (BaseConfig)f.get(this);
            if (config == null) {
                return null;
            }

            return aClass.getDeclaredConstructor(Context.class, BaseConfig.class).
                    newInstance(context, config);
        } catch (Exception e) {
            Logger.e(TAG, "get live icon failed", e);
            return null;
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Set<String> getLiveIcons() {
        return liveIcons;
    }

    public void setLiveIcons(Set<String> liveIcons) {
        this.liveIcons = liveIcons;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public ClockText getClockText() {
        return clockText;
    }

    public void setClockText(ClockText clockText) {
        this.clockText = clockText;
    }

    public ClockDial getClockDial() {
        return clockDial;
    }

    public void setClockDial(ClockDial clockDial) {
        this.clockDial = clockDial;
    }

    @Override
    public String toString() {
        return "LiveIcons{" +
                "version=" + version +
                ", liveIcons=" + liveIcons +
                ", calendar=" + calendar +
                ", clockText=" + clockText +
                ", clockDial=" + clockDial +
                '}';
    }

    public static class Text {
        private int size;
        private String color;
        private String font;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getFont() {
            return font;
        }

        public void setFont(String font) {
            this.font = font;
        }

        public Paint toPaint(Context context) {
            return toPaint(context, 0);
        }

        public Paint toPaint(Context context, int flag) {
            Paint paint = new Paint(flag);
            paint.setTextSize(size * Utils.getDensity(context));
            paint.setTypeface(FontCache.get(context, font));
            paint.setColor(Color.parseColor(color));

            return paint;
        }

        @Override
        public String toString() {
            return "Text{" +
                    "size=" + size +
                    ", color='" + color + '\'' +
                    ", font='" + font + '\'' +
                    '}';
        }
    }

    public static class BaseConfig {
        private String background;
        private Set<String> components;

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public Set<String> getComponents() {
            return components;
        }

        public void setComponents(Set<String> components) {
            this.components = components;
        }

        @Override
        public String toString() {
            return "BaseConfig{" +
                    "background='" + background + '\'' +
                    ", components=" + components +
                    '}';
        }
    }

    public static class Calendar extends BaseConfig {
        private float paddingTop;
        private Text dayText;
        private Text weekText;

        public float getPaddingTop() {
            return paddingTop;
        }

        public void setPaddingTop(float paddingTop) {
            this.paddingTop = paddingTop;
        }

        public Text getDayText() {
            return dayText;
        }

        public void setDayText(Text dayText) {
            this.dayText = dayText;
        }

        public Text getWeekText() {
            return weekText;
        }

        public void setWeekText(Text weekText) {
            this.weekText = weekText;
        }

        @Override
        public String toString() {
            return "Calendar{" +
                    "background='" + getBackground() + '\'' +
                    ", components=" + getComponents() +
                    ", paddingTop=" + paddingTop +
                    ", dayText=" + dayText +
                    ", weekText=" + weekText +
                    '}';
        }
    }

    public static class ClockText extends BaseConfig {
        private float paddingTop;
        private Text hourText;
        private Text minuteText;
        private Text semiText;

        public Text getHourText() {
            return hourText;
        }

        public void setHourText(Text hourText) {
            this.hourText = hourText;
        }

        public Text getMinuteText() {
            return minuteText;
        }

        public void setMinuteText(Text minuteText) {
            this.minuteText = minuteText;
        }

        public Text getSemiText() {
            return semiText;
        }

        public void setSemiText(Text semiText) {
            this.semiText = semiText;
        }

        public float getPaddingTop() {
            return paddingTop;
        }

        public void setPaddingTop(float paddingTop) {
            this.paddingTop = paddingTop;
        }

        @Override
        public String toString() {
            return "ClockText{" +
                    "background='" + getBackground() + '\'' +
                    ", components=" + getComponents() +
                    ", paddingTop=" + paddingTop +
                    ", hourText=" + hourText +
                    ", minuteText=" + minuteText +
                    ", secondText=" + semiText +
                    '}';
        }
    }

    public static class ClockDial extends BaseConfig {
        private String hour;
        private String minute;
        private String second;

        public String getHour() {
            return hour;
        }

        public void setHour(String hour) {
            this.hour = hour;
        }

        public String getMinute() {
            return minute;
        }

        public void setMinute(String minute) {
            this.minute = minute;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second;
        }

        @Override
        public String toString() {
            return "ClockDial{" +
                    "background='" + getBackground() + '\'' +
                    ", components=" + getComponents() +
                    ", hour='" + hour + '\'' +
                    ", minute='" + minute + '\'' +
                    ", second='" + second + '\'' +
                    '}';
        }
    }
}
