package com.qicode.imageloaderdr.util;

import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;

/**
 * Created by chenming on 16/9/26.
 */
public class ImageSizeUtil {
    /**
     * 获得imageview的宽高
     * @param imageView
     * @return
     */
    public static ImageSize getImageViewSize(ImageView imageView){
        ImageSize result = new ImageSize();

        DisplayMetrics displayMetrics = imageView.getContext().getResources()
                .getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        /**
         * step1:获得实际宽度
         * step2:获得布局指定宽度
         * step3:获得mMaxWidth
         * step4:获得屏幕宽度
         * 高度也做同样处理
         */
        //高度
        int width = imageView.getWidth();//step1
        if(width <= 0){
            width = lp.width;//step 2
        }

        if(width <= 0){
            width = getMaxWidth(imageView);//step 3
        }

        if(width <= 0){
            width = displayMetrics.widthPixels;//step 4
        }

        //高度
        int height = imageView.getHeight();//step1
        if(height <= 0){
            height = lp.height;//step 2
        }

        if(height <= 0){
            height = getMaxHeight(imageView);//step 3
        }

        if(height <= 0){
            height = displayMetrics.heightPixels;//step 4
        }

        result.width = width;
        result.height = height;
        return result;
    }

    /**
     * 反射获得最大宽度
     * @param imageView
     * @return
     */
    private static int getMaxWidth(ImageView imageView) {
        try {
            Class clazz = Class.forName("android.widget.ImageView");
            Field field = clazz.getDeclaredField("mMaxWidth");
            field.setAccessible(true);
            return field.getInt(imageView);
        } catch (ClassNotFoundException e) {

        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 反射获得最大高度
     * @param imageView
     * @return
     */
    private static int getMaxHeight(ImageView imageView) {
        try {
            Class clazz = Class.forName("android.widget.ImageView");
            Field field = clazz.getDeclaredField("mMaxHeight");
            field.setAccessible(true);
            return field.getInt(imageView);
        } catch (ClassNotFoundException e) {

        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int caculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                           int reqHeight){

        int rawWidth = options.outWidth;
        int rawHeigh = options.outHeight;

        int sampleRatio = 1;

        if (rawWidth > reqWidth || rawHeigh > reqHeight){
            int widthRatio = Math.round(rawWidth * 1.0f / reqWidth);
            int heightRatio = Math.round(rawHeigh * 1.0f / reqHeight);
            sampleRatio = Math.max(widthRatio, heightRatio);
        }
        return sampleRatio;
    }
}
