package com.nhj.server;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {

    private Context context ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    /**
     * 加载未安装的apk
     * @param view
     */
    public void loadunInstalledApk(View view) {
        String pathSd = getSDPath() ;
        Log.i("@@","pathSd = "+pathSd) ;
        String apkPath = pathSd+File.separator+"app-debug.apk" ;
        File f = new File(apkPath) ;
        Log.i("@@","文件是否存在 f = "+f.exists());//6.0及以上的手机上需要在设置里面打开该app的读写权限，所以下载的插件最好还是放入内置存储
       PackageManager pm =  getPackageManager() ;
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkPath,PackageManager.GET_ACTIVITIES) ;
       String pluginPkgName = packageInfo.packageName ;
       Log.i("@@",pluginPkgName) ;
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method mt = AssetManager.class.getMethod("addAssetPath",String.class);
            mt.invoke(assetManager,apkPath) ;
            Resources supRes = getResources() ;
            //通过反射构造插件assetManager对象，在通过它生成mResources对象来访问插件app中的资源文件
            Resources mResources = new Resources(assetManager,supRes.getDisplayMetrics(),supRes.getConfiguration()) ;
//            public DexClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent)
            File dexOutput = getDir("dex",Context.MODE_PRIVATE) ;
            DexClassLoader dexClassLoader = new DexClassLoader(apkPath,dexOutput.toString(),null,ClassLoader.getSystemClassLoader());
            Class clazz = dexClassLoader.loadClass(pluginPkgName+".R$mipmap") ;//注意为：classPath
            Field field = clazz.getField("guide_poll_img") ;
            int resourceId = field.getInt(clazz) ;
            ((ImageView)findViewById(R.id.img)).setImageDrawable(mResources.getDrawable(resourceId));
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
    public String getSDPath(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if(sdCardExist)
        {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }

    public void loadApk(View view) {
        List<PluginBean> ls = findAllPlugins() ;
        Context context = null;
        if(ls.size()<=0){

            Toast.makeText(getApplicationContext(),"没找到",Toast.LENGTH_LONG).show();
        }else{
            try {
                context = this.createPackageContext(ls.get(0).pkgName,CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE) ;
                int recourseId = dynamicLoadApk(ls.get(0).pkgName,context);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageView)findViewById(R.id.img)).setImageDrawable(context.getDrawable(recourseId));
                }else {
                    Log.i("@@","aaaaaa") ;
                    ((ImageView)findViewById(R.id.img)).setImageResource(recourseId);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
    }


    public int dynamicLoadApk(String packageName , Context context) throws ClassNotFoundException, NoSuchFieldException {
        if (context!=null){
            this.context = context ;
            Log.i("@@",context.getPackageResourcePath());
            PathClassLoader pClassLoader = new PathClassLoader(context.getPackageResourcePath(),ClassLoader.getSystemClassLoader());
            Class clazz = Class.forName(packageName+".R$mipmap",true,pClassLoader) ;
            Field cField  = clazz.getDeclaredField("channel_new");
            try {
                int recourseId = cField.getInt(R.mipmap.class) ;
                return recourseId ;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
    public List<PluginBean> findAllPlugins(){
       PackageManager pm = this.getPackageManager() ;
        List<PluginBean> ps = new ArrayList<>() ;
       List<PackageInfo> lists = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
       for(int i=0;i<lists.size();i++){
           PackageInfo appInfo = lists.get(i) ;
           String packageName = appInfo.packageName ;
           String sharedUserId = appInfo.sharedUserId ;
           if(!TextUtils.isEmpty(sharedUserId)&&"com.nhj.smart".equals(sharedUserId)&&(!getPackageName().equals(packageName))){
               String label = pm.getApplicationLabel(appInfo.applicationInfo).toString() ;
               PluginBean pluginBean = new PluginBean(packageName,label) ;
               ps.add(pluginBean) ;
           }else{

           }
       }
       return ps ;
    }




    static class PluginBean{

        private  String pkgName;
        private  String label;

        public PluginBean(String pkgName , String label){
            this.pkgName = pkgName ;
            this.label = label;
        }
    }

}
