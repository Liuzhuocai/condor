package com.condor.launcher.predictor;

import java.util.List;

/**
 * Created by Perry on 19-1-22
 */
public class PredictorApps {
    private int version;
    private List<String> apps;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<String> getApps() {
        return apps;
    }

    public void setApps(List<String> apps) {
        this.apps = apps;
    }
}
