package com.qicode.imageloaderdr;

import android.app.Application;

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
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ImageLoader.getInstance(this).quit();
    }
}
