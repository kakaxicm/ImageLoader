package com.qicode.imageloaderdr;

import android.app.Application;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.qicode.imageloaderdr.imageloader.ImageLoader;

/**
 * Created by chenming on 16/11/30.
 */

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
        Log.e("TAG", "onCreate");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.e("TAG", "onTrimMemory");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e("TAG", "onTerminate");
    }
}
