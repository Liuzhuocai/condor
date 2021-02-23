package com.condor.launcher.theme;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import com.android.launcher3.LauncherModel;
import com.android.launcher3.R;
import com.condor.launcher.theme.bean.ThemeConfigBean;
import com.condor.launcher.theme.bean.ThemeDescriptionBean;
import com.condor.launcher.theme.utils.ZIPThemeConfigParseByPull;
import com.condor.launcher.util.ThemeUtils;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class ExternalIconGetter implements IIcongetter {
    Context mContext;
    String localpath;
    private ThemeDescriptionBean tdb;
    private ThemeConfigBean tcb;
    private int  mThemeType = -1;

    private BitmapDrawable mMask;
    private BitmapDrawable mShadow;


    public static String TAG = ExternalIconGetter.class.getSimpleName();
    protected HashMap<String, String> mLabel_Icons = new HashMap<String,String>();
    public ExternalIconGetter(Context context, int type) {
        mContext = context.getApplicationContext();
        Log.d("liuzuo99","ExternalIconGetter  type="+type);
        mThemeType = type;
        switch (type){
            case  ThemeConfig.Type_external:
                localpath = ThemeConfig.extenal_Theme_Folder;
                break;

            case  ThemeConfig.Type_external_v5:
                localpath = ThemeConfig.extenal_Theme_Folder_v5;
                break;
            default:
                localpath = ThemeConfig.default_Theme_Folder_Icons;
        }
        //localpath = (type == ThemeConfig.Type_external) ? ThemeConfig.extenal_Theme_Folder : ThemeConfig.default_Theme_Folder;
        if(isThemeV5()){
            LauncherModel.runOnWorkerThread(() -> initTheme(context));
        }
    }

    private void initTheme(Context context) {
        AssetManager assets = null;
        InputStream dp = null;
        InputStream cf = null;
        String descriptionPath = null;
        String configPath = null;
        String iconConfigPath = null;
        switch (mThemeType){
            case ThemeConfig.Type_external_v5 :
                descriptionPath = ThemeConfig.extenal_Theme_Description;
                configPath = ThemeConfig.extenal_Theme_Config;
                iconConfigPath = ThemeConfig.extenal_Theme_Icon_Config;
                break;
            case ThemeConfig.Type_default :
                descriptionPath = ThemeConfig.default_Theme_Description;
                configPath = ThemeConfig.default_Theme_Config;
                iconConfigPath = ThemeConfig.default_Theme_Icon_Config;
                break;
            default:
        }
        if (descriptionPath==null)
            return;


        try {
            File f1 = new File(descriptionPath);
            File f2 = new File(configPath);
            if(f2.exists()) {
                cf = new FileInputStream(f2);
                if(f1.exists()){
                    dp = new FileInputStream(f1) ;
                }
                Log.d("liuzuo99","extenal_Theme_Config");
            }else {
                assets = mContext.getAssets();
                dp = assets.open(ThemeConfig.default_Theme_Description);
                cf = assets.open(ThemeConfig.default_Theme_Config);
                Log.d("liuzuo99","default_Theme_Config");
            }




            XStream tdbStream = new XStream();
            tdbStream.alias("ThemeDescriptionBean",ThemeDescriptionBean.class);
            //tdbStream.omitField(ThemeDescriptionBean.class,"names");

            XStream tcbStream = new XStream();
            tcbStream.alias("ThemeConfigBean",ThemeConfigBean.class);
            //tcbStream.omitField(ThemeConfigBean.class,"icon_coordinate");
            //tcbStream.omitField(ThemeConfigBean.class,"folder_coordernate");

            if(dp!=null){
                //tdb = (ThemeDescriptionBean) tdbStream.fromXML(dp);
            }
            tcb = (ThemeConfigBean) tcbStream.fromXML(cf);
            ThemeUtils.normalizatiionThemeConfigBean(context,tcb);
            //Log.d("liuzuo99","ExternalIconGetter ThemeDescriptionBean="+tdb.toString()+"    ThemeConfigBean="+tcb.toString());

            ZIPThemeConfigParseByPull tp = new ZIPThemeConfigParseByPull();
            InputStream instream = null;
            try {
                File file = new File(iconConfigPath);
                if (file.exists()) {
                    instream = new FileInputStream(iconConfigPath);
                    if (instream == null) {
                        Log.e(TAG, "can't create inputStream");
                        return ;
                    }
                    tp.parse(instream);
                } else {
                    Log.e(TAG, "can't find config.xml path : " + iconConfigPath);
                    return ;
                }
                if (!tp.hasData()) return ;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "init IOException : " + e.toString());
                return ;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "init Exception : " + e.toString());
                return ;
            }finally {
                if (null != instream) {
                    try {
                        instream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            mLabel_Icons = tp.getmLabel_Icons();
            Log.d("liuzuo99","mLabel_Icons="+mLabel_Icons.size());
        } catch (Exception e) {
            Toast.makeText(mContext, R.string.theme_config_read_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            Log.d("liuzuo99","printStackTrace=",e);
        }finally {
            if (null != dp) {
                try {
                    dp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != cf) {
                try {
                    cf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Drawable getShadow(){
       /* Bitmap b = getBitmapByName("shadow.png");
        Log.i("theme1","getShadow : b = "+b);*/
       if(mShadow==null){
           if(isThemeV5()){
               mShadow = new BitmapDrawable(mContext.getResources(),getBitmapByName("shadow.png"));
           }else {
               mShadow = new BitmapDrawable(mContext.getResources(),getBitmapByName("shadow.jpg"));
           }
       }
        return mShadow;
    }

    private Drawable getMask(){
       /* Bitmap b = getBitmapByName("mask.png");
        Log.i("theme1","getShadow : b = "+b);*/
        if(mMask==null){
            Bitmap bitmapByName = null;
            if(isThemeV5()){
                bitmapByName = getBitmapByName("mask.png");
            }else {
                bitmapByName = getBitmapByName("mask.jpg");
            }

            if(bitmapByName==null){
                return getShadow();
            }
            mMask =new BitmapDrawable(mContext.getResources(),bitmapByName);
        }
        return mMask;
    }
    @Override
    public Drawable getIconDrawable(LauncherActivityInfo info, int iconDpi) {
        if(mLabel_Icons != null&&mLabel_Icons.size()>0){
        Drawable d =null;
        Bitmap b = null;
        String pkg = info.getComponentName().getPackageName();
        String cls = info.getComponentName().getClassName();
        if(pkg!=null){
            if(cls!=null){
                String path = mLabel_Icons.get(pkg + "_" + cls);
                Log.d("liuzuo88",pkg + "_" + cls+"   info="+info.getLabel());
                if(path!=null){
                    b = getBitmapByName(path+".png");
                }
            }else {
                String path = mLabel_Icons.get(pkg);
                if(path!=null){
                    b = getBitmapByName(path+".png");
                }
            }
        }

         //b = getBitmapByName(info.getComponentName().getPackageName()+"_"+info.getComponentName().getClassName()+".jpg");
        if(b==null){
            d = info.getIcon(iconDpi);
            d  = composite(d,getShadow(),getMask());
        }else{
            d = new BitmapDrawable(mContext.getResources(),b);
            //d  = composite(d,getShadow(),getShadow());
        }
        return d;
        }else {
            Drawable d =null;
            Bitmap b = getBitmapByName(info.getComponentName().getPackageName()+"_"+info.getComponentName().getClassName()+".jpg");
            if(b==null){
                d = info.getIcon(iconDpi);
                d  = composite(d,getShadow(),getMask());
            }else{
                d = new BitmapDrawable(mContext.getResources(),b);
                //d  = composite(d,getShadow(),getShadow());
            }
            return d;
        }
    }

    private Drawable composite(Drawable source, Drawable bg, Drawable mask) {
        if (bg != mask) {
            if (source == null || bg == null || mask == null) return null;
            if (!(isValidate(source) && isValidate(bg) && isValidate(mask))) return null;
            Log.i("theme1", "bg:( " + bg.getIntrinsicWidth() + "  ,  " + bg.getIntrinsicHeight() + " )");
            int bgWidth = bg.getIntrinsicWidth();
            int bgHeight = bg.getIntrinsicHeight();
            int maskWidth = mask.getIntrinsicWidth();
            int maskHeight = mask.getIntrinsicHeight();
            Bitmap bitmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            canvas.save();
            source.setBounds(0, 0, maskWidth, maskHeight);
            source.draw(canvas);
            canvas.restore();


            canvas.save();
            Bitmap maskB = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888);
            Canvas canvasMask = new Canvas(maskB);
            mask.setBounds(0, 0, maskWidth, maskHeight);
            mask.draw(canvasMask);
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvasMask.save();
            canvasMask.drawBitmap(bitmap, 0, 0, paint);
            canvasMask.restore();


            Bitmap bitmapResult = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
            Canvas canvasBg = new Canvas(bitmapResult);
            bg.setBounds(0, 0, bgWidth, bgHeight);
            bg.draw(canvasBg);
            canvasBg.save();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            int left = 0;
            int top = 0;
            if (bgWidth > maskWidth) {
                left = (bgWidth - maskWidth) / 2;
            }
            if (bgHeight > maskHeight) {
                top = (bgHeight - maskHeight) / 2;
            }
            if (tcb != null) {
                left += tcb.getCoordinate_x();
                top += tcb.getCoordinate_y();
            }
            canvasBg.drawBitmap(maskB, left, top, paint);
            canvasBg.restore();

            return new BitmapDrawable(mContext.getResources(), bitmapResult);

        } else {
            if (source == null || bg == null || mask == null) return null;
            if (!(isValidate(source) && isValidate(bg) && isValidate(mask))) return null;
            Log.i("theme1", "bg1:( " + bg.getIntrinsicWidth() + "  ,  " + bg.getIntrinsicHeight() + " )");
            Bitmap bitmap = Bitmap.createBitmap(bg.getIntrinsicWidth(), bg.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.GREEN);
            bg.setBounds(0, 0, bg.getIntrinsicWidth(), bg.getIntrinsicWidth());
            bg.draw(canvas);
            canvas.save();
            source.setBounds(0, 0, bg.getIntrinsicWidth(), bg.getIntrinsicHeight());
            source.draw(canvas);
            canvas.restore();
            canvas.save();
            Bitmap maskB = Bitmap.createBitmap(mask.getIntrinsicWidth(), mask.getMinimumHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasMask = new Canvas(maskB);
            canvas.save();
            mask.setBounds(0, 0, bg.getIntrinsicWidth(), bg.getIntrinsicHeight());
            mask.draw(canvasMask);
            canvas.restore();
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvasMask.save();
            canvasMask.drawBitmap(bitmap, 0, 0, paint);
            canvasMask.restore();
            return new BitmapDrawable(mContext.getResources(), maskB);
        }
    }

    private boolean isValidate(Drawable d){
        return d!=null&&d.getIntrinsicHeight()>0&&d.getIntrinsicWidth()>0;
    }
    @Override
    public Bitmap getBitmapByName(String iconName) {
        Bitmap result = null;
        FileInputStream fin = null;
        try {
            String iconPath = localpath+"/" + iconName;
            Log.i("theme","iconPath = "+iconPath);
            File f = new File(iconPath);
            if(f.exists()){
                Log.i("theme","iconPath exists");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                fin = new FileInputStream(f);
                result = BitmapFactory.decodeStream(fin, null, options).copy(Bitmap.Config.ARGB_8888, true);
            }
        } catch (Exception e) {
            Log.i("theme","iconPath not exists");
            return null;
        } finally {
            if (null != fin) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }

    @Override
    public ThemeConfigBean getThemeConfigBean() {
        return tcb;
    }

    @Override
    public ThemeDescriptionBean getThemeDescriptionBean() {
        return tdb;
    }

    private boolean isThemeV5(){
        return mThemeType == ThemeConfig.Type_external_v5 ||mThemeType == ThemeConfig.Type_default ;
    }
}
