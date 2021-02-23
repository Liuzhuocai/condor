package com.condor.launcher.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.android.launcher3.IconProvider;
import com.condor.launcher.theme.bean.ThemeConfigBean;
import com.condor.launcher.theme.bean.ThemeDescriptionBean;

import java.io.File;

public class ThemeIconProvider extends IconProvider {
    private Context mContext;
    private IIcongetter mIcongetter;
    public ThemeIconProvider(Context context){
        mContext = context;
        init();
    }

    public void init(){
        if(isThemeAvailableOnExternalV5()){
            mIcongetter = new ExternalIconGetter(mContext, ThemeConfig.Type_external_v5);
        }else if(isThemeAvailableOnExternal()){
            mIcongetter = new ExternalIconGetter(mContext, ThemeConfig.Type_external);

        }else if(isThemeAvailableOnDefault()){
            mIcongetter = new ExternalIconGetter(mContext, ThemeConfig.Type_default);
        } else if(isThemeAvailableOnLauncher()){
            mIcongetter = new InternalIcongetter(mContext);
        }else{
            mIcongetter=null;
        }
    }

    @Override
    public void reInit() {
        init();
    }

    @Override
    public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
        Drawable drawable = mIcongetter.getIconDrawable(info,iconDpi);
        return drawable==null?super.getIcon(info, iconDpi, flattenDrawable):drawable;
    }

    public static boolean isThemeAvailableOnExternal(){
        String themeFolderPath = ThemeConfig.extenal_Theme_Folder+"/shadow.jpg";
        File file =new File(themeFolderPath);
        Log.i("theme1","themeFolderPath = "+themeFolderPath+"   fuck : "+(file.isFile()&&file.canRead()));
        return file.isFile()&&file.canRead();
    }
    private boolean isThemeAvailableOnExternalV5(){
        File file =new File(ThemeConfig.extenal_Theme_Folder_v5+"/shadow.png");
        return file.isFile()&&file.canRead();
    }
    private boolean isThemeAvailableOnDefault(){

        File file =new File(ThemeConfig.default_Theme_Config);
        return file.isFile()&&file.canRead();
    }

    private boolean isThemeAvailableOnLauncher(){
        return true;
    }

    public boolean isDefaultTheme(){
        return (mIcongetter instanceof  InternalIcongetter);
    }

    public static long  getThemeModifyTime(){
        String themeFolderPath = ThemeConfig.extenal_Theme_Folder+"/shadow.jpg";
        File file =new File(themeFolderPath);
        String themeFolderPath1 = ThemeConfig.extenal_Theme_Folder_v5+"/shadow.png";
        File file1=new File(themeFolderPath1);
        long lastModified = file.lastModified();
        long lastModified1 = file1.lastModified();
        return lastModified>lastModified1?lastModified:lastModified1;
    }

    @Override
    public boolean isThemeChanged() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("com.android.launcher3.prefs", Context.MODE_PRIVATE);
        boolean isDefaultTheme = sharedPreferences.getBoolean("isDefaultTheme",true);
        long modifyTime = sharedPreferences.getLong("modifyTime",0);

        boolean isCurrentDefaultTheme = !(isThemeAvailableOnExternal()||isThemeAvailableOnExternalV5());
        long currentThemeModifyTime =getThemeModifyTime();
        Log.i("theme1","isCurrentDefaultTheme  = "+isCurrentDefaultTheme+" , isDefaultTheme = "+isDefaultTheme);
        if(!isCurrentDefaultTheme){
            Log.i("theme1","modifyTime = "+modifyTime+"  , "+" , currentThemeModifyTime = "+currentThemeModifyTime);
            if(modifyTime!=currentThemeModifyTime){
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isDefaultTheme",false);
                editor.putLong("modifyTime",currentThemeModifyTime);
                editor.apply();
                Log.i("theme1","xxxxxxxxxxxxxxxxxxxxxxxxxx");
                return true;
            }else{
                return false;
            }
        }else{
            if(!isDefaultTheme){
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isDefaultTheme",true);
                editor.putLong("modifyTime",0);
                editor.apply();
                Log.i("theme1","xxxxxxxxxxxxxxxxxxxxxxxxxx2222");
                return true;
            }else{
                return false;
            }
        }
    }
    public ThemeConfigBean getThemeConfigBean() {
        if (mIcongetter==null)
            return null;
        return mIcongetter.getThemeConfigBean();
    }
    public ThemeDescriptionBean getThemeDescriptionBean() {
        if (mIcongetter==null)
            return null;
        return mIcongetter.getThemeDescriptionBean();
    }
    public Bitmap getBitmapByName(String iconName) {
        if (mIcongetter==null)
            return null;
        return mIcongetter.getBitmapByName(iconName);
    }
}
