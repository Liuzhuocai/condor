package com.condor.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;

/**
 * add for setting workspace app ranking rows and numbers by zengweizhong on 2018.08.13
 */

public class UnreadMessageActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private ImageView ivBack;
    private LinearLayout mUnreadPhone, mUnreadSms;
    private Switch mSwitchUnreadPhone, mSwitchUnreadSms;
    public static final String UNREAD_PHONE_PREF = "pref_unread_phone_settings";
    public static final String UNREAD_SMS_PREF = "pref_unread_sms_settings";
    private static int SMS_REQUEST_CODE = 1011;
    private static int CALL_LOG_REQUEST_CODE = 1022;

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_condor_unread_message);
        initView();

    }

    private void initView() {
        ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        mUnreadPhone = findViewById(R.id.ll_unread_call);
        mUnreadPhone.setOnClickListener(this);

        mUnreadSms = findViewById(R.id.ll_unread_sms);
        mUnreadSms.setOnClickListener(this);

        mSwitchUnreadPhone = (Switch) findViewById(R.id.switch_unread_call);

        if (Utilities.isPermissionGranted(this, Manifest.permission.READ_CALL_LOG)) {
            mSwitchUnreadPhone.setChecked(CustomTools.mSwitchUnreadPhone);
        }else {
            CustomTools.mSwitchUnreadPhone = false;
        }
        mSwitchUnreadPhone.setOnCheckedChangeListener(this);

        mSwitchUnreadSms = (Switch) findViewById(R.id.switch_unread_sms);
        if (Utilities.isPermissionGranted(this, Manifest.permission.READ_SMS)) {
            mSwitchUnreadSms.setChecked(CustomTools.mSwitchUnreadSms);
        }else {
            CustomTools.mSwitchUnreadSms = false;
        }

        mSwitchUnreadSms.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.ll_unread_call:
                mSwitchUnreadPhone.setChecked(!CustomTools.mSwitchUnreadPhone);
                break;
            case R.id.ll_unread_sms:
                mSwitchUnreadSms.setChecked(!CustomTools.mSwitchUnreadSms);
                break;

            default:
                break;
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_unread_sms:
                if (isChecked) {
                    if (Utilities.isPermissionGranted(this, Manifest.permission.READ_SMS)) {
                        CustomTools.mSwitchUnreadSms = isChecked;
                        Utilities.getPrefs(this).edit().putBoolean(UNREAD_SMS_PREF, isChecked).commit();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_REQUEST_CODE);
                    }
                } else {
                    CustomTools.mSwitchUnreadSms = isChecked;
                    Utilities.getPrefs(this).edit().putBoolean(UNREAD_SMS_PREF, isChecked).commit();
                }

                break;
            case R.id.switch_unread_call:
                if (isChecked) {
                    if (Utilities.isPermissionGranted(this, Manifest.permission.READ_CALL_LOG)) {
                        CustomTools.mSwitchUnreadPhone = isChecked;
                        Utilities.getPrefs(this).edit().putBoolean(UNREAD_PHONE_PREF, isChecked).commit();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, CALL_LOG_REQUEST_CODE);
                    }
                } else {
                    CustomTools.mSwitchUnreadPhone = isChecked;
                    Utilities.getPrefs(this).edit().putBoolean(UNREAD_PHONE_PREF, isChecked).commit();
                }

                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i]  == PackageManager.PERMISSION_GRANTED) {
                    CustomTools.mSwitchUnreadSms = true;
                    Utilities.getPrefs(this).edit().putBoolean(UNREAD_SMS_PREF, true).commit();
                } else {
                    mSwitchUnreadSms.setChecked(false);
                }
            }

        } else if (requestCode == CALL_LOG_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i]  == PackageManager.PERMISSION_GRANTED) {
                    CustomTools.mSwitchUnreadPhone = true;
                    Utilities.getPrefs(this).edit().putBoolean(UNREAD_PHONE_PREF, true).commit();
                } else {
                    mSwitchUnreadPhone.setChecked(false);
                }
            }
        }
    }

}
