package com.qicode.imageloaderdr.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.qicode.imageloaderdr.util.BitmapUtils;
import com.qicode.imageloaderdr.util.ImageSize;
import com.qicode.imageloaderdr.util.ImageSizeUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by chenming on 16/9/26.
 * 图片下载器,图片下载线程的执行代码
 */
public class ImageDownloader {
    /**
     * 无硬盘缓存,下载图片到内存
     * @param urlStr
     * @param imageView
     * @return
     * @throws IOException
     */
    public static Bitmap downloadImgFromUrl(String urlStr, ImageView imageView) {
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(connection.getInputStream());
            is.mark(1024*1024);
            //获得网络图片的宽高
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

            ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
            opts.inSampleSize = ImageSizeUtil.caculateInSampleSize(opts, imageSize.width, imageSize.height);

            opts.inJustDecodeBounds = false;
            is.reset();
            bitmap = BitmapFactory.decodeStream(is, null, opts);
            connection.disconnect();
            return bitmap;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }

        }
        return null;
    }

    /**
     * 下载图片到硬盘
     * @param urlStr
     * @param imageView
     * @return
     * @throws IOException
     */
    public static Bitmap downloadImgFromUrlToFile(String urlStr, ImageView imageView, String fileName) {
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(connection.getInputStream());
            is.mark(1024*1024);
            //获得网络图片的宽高
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

            ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
            opts.inSampleSize = ImageSizeUtil.caculateInSampleSize(opts, imageSize.width, imageSize.height);

            opts.inJustDecodeBounds = false;
            is.reset();
            bitmap = BitmapFactory.decodeStream(is, null, opts);
            BitmapUtils.saveBitmap(imageView.getContext(), bitmap, fileName);
            connection.disconnect();
            return bitmap;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }

        }
        return null;
    }
}
