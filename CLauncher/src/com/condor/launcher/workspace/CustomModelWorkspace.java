package com.condor.launcher.workspace;

/**
 * Bruce: add for custom workspace profile
 */
public class CustomModelWorkspace {

    private String modelPhone;
    private String profilesName;

    public CustomModelWorkspace() {
    }

    public String getModelPhone() {
        return modelPhone;
    }

    public void setModelPhone(String modelPhone) {
        this.modelPhone = modelPhone;
    }

    public String getProfilesName() {
        return profilesName;
    }

    public void setProfilesName(String profilesName) {
        this.profilesName = profilesName;
    }

    @Override
    public String toString() {
        return "CustomModelWorkspace{" +
                "modelPhone='" + modelPhone + '\'' +
                ", profilesName='" + profilesName + '\'' +
                '}';
    }
}
