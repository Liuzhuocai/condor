package com.condor.launcher.workspace;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.android.launcher3.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;


/**
 * Bruce : add for custom workspace profile
 */
public class WorkspaceParser {

    public List<CustomModelWorkspace> parse(Context context) throws Exception {
        List<CustomModelWorkspace> modelWorkspaceList = null;
        CustomModelWorkspace modelWorkspace = null;

        XmlResourceParser xrp = context.getResources().getXml(R.xml.condor_custom_device_workspace);

        int eventType = xrp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    modelWorkspaceList = new ArrayList<CustomModelWorkspace>();
                    break;

                case XmlPullParser.START_TAG:
                    if (xrp.getName().equals("model_woekspace")) {
                        modelWorkspace = new CustomModelWorkspace();
                    } else if (xrp.getName().equals("model_phone")) {
                        eventType = xrp.next();
                        modelWorkspace.setModelPhone(xrp.getText());

                    } else if (xrp.getName().equals("profiles_name")) {
                        eventType = xrp.next();
                        modelWorkspace.setProfilesName(xrp.getText());
                    }
                    break;

                case XmlPullParser.END_TAG:
                    if (xrp.getName().equals("model_woekspace")) {
                        modelWorkspaceList.add(modelWorkspace);
                        modelWorkspace = null;
                    }
                    break;
            }
            eventType = xrp.next();
        }
        return modelWorkspaceList;
    }
}
