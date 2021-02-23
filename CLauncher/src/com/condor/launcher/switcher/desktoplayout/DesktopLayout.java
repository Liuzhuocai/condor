package com.condor.launcher.switcher.desktoplayout;

import com.android.launcher3.InvariantDeviceProfile;
import com.condor.launcher.switcher.SwitchEntry;

import java.util.Locale;

/**
 * Created by Perry on 19-1-14
 */
public class DesktopLayout extends SwitchEntry {
    private static final String FORMAT = "%dx%d";
    private static final String VALUE_FORMAT = "desktop_layout_%dx%d";
    private final InvariantDeviceProfile idp;

    public DesktopLayout(InvariantDeviceProfile idp) {
        super(String.format(Locale.ENGLISH, VALUE_FORMAT, idp.numRows, idp.numColumns));
        this.idp = idp;
    }

    public String getEntry() {
        return String.format(Locale.ENGLISH, FORMAT, idp.numRows, idp.numColumns);
    }

    public InvariantDeviceProfile getIdp() {
        return idp;
    }
}
