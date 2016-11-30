package com.qicode.imageloaderdr.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by chenming on 16/10/27.
 */

public class BitmapUtils {
    /**
     * 将bitmap存成文件
     */
    public static String saveBitmap(Context context, Bitmap bitmap, String fileName) {
        if (context == null || bitmap == null || TextUtils.isEmpty(fileName)) {
            return null;
        }
        String ret = null;
        OutputStream fos = null;
        try {
            Uri uri = Uri.fromFile(new File(BitmapUtils.getTempSaveDir(context) + fileName));
            File file = new File(uri.getPath());
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fos = context.getContentResolver().openOutputStream(uri);
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            ret = getTempSaveDir(context) + fileName;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * 获得缓存目录路径
     *
     * @param context
     * @return
     */
    public static String getTempSaveDir(Context context) {
        String userDir;
        userDir = context.getCacheDir().getAbsolutePath();
        File file = new File(userDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath() + "/";
    }

    /**
     * 将字符串转成MD5值
     */
    public static String toMD5(String string) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();
    }

}
