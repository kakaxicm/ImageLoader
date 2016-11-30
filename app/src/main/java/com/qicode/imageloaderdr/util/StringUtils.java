package com.qicode.imageloaderdr.util;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huyongsheng on 2014/7/18.
 */
public class StringUtils {

    /**
     * 生成google play连接地址
     */
    public static String getGooglePlayString(Activity activity, String packageName) {
        return getGooglePlayString(packageName, "flip", activity.getPackageName());
    }

    /**
     * 生成google play连接地址
     */
    public static String getGooglePlayString(String packageName, String source, String medium) {
        return StringUtils
                .getString("market://details?id=", packageName, "&referrer=", "utm_source%3D", source, "%26utm_medium%3D",
                        medium);
    }

    /**
     * 最优化String的构建
     */
    public static String getString(Object... objects) {
        StringBuffer buffer = new StringBuffer();
        for (Object object : objects) {
            buffer.append(object);
        }
        return buffer.toString();
    }

    /**
     * 得到配置文件中的MetaData数据
     */
    public static String getMetaData(Context context, String keyName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = info.metaData;
            Object value = bundle.get(keyName);
            if (value != null) {
                return value.toString();
            } else {
                return null;
            }
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    /**
     * 获取package信息
     */
    public static PackageInfo getPackageInfo(Context context) throws NameNotFoundException {
        return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    }

    /**
     * 生成base64编码
     */
    public static String encodeBase64(String string) {
        return Base64.encodeToString(string.getBytes(), Base64.NO_WRAP);
    }

    /**
     * base64解码
     */
    public static String decodeBase64(String string) {
        String result = null;
        if (!StringUtils.isNullOrEmpty(string)) {
            try {
                result = new String(Base64.decode(string, Base64.NO_WRAP), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 对double数据进行截断
     */
    public static String cutDouble0(double value) {
        DecimalFormat format = new DecimalFormat("##0");
        return format.format(value);
    }

    /**
     * 对double数据进行截断
     */
    public static String cutFloat0(float value) {
        DecimalFormat format = new DecimalFormat("##0");
        return format.format(value);
    }

    /**
     * 判断String是否为空
     */
    public static boolean isNullOrEmpty(String inputString) {
        return null == inputString || inputString.trim().equals("");
    }

    /**
     * 判断bytes是否为空
     */
    public static boolean isNullOrEmpty(byte[] bytes) {
        return null == bytes || bytes.length == 0;
    }

    /**
     * 获取post请求中的参数
     */
    public static String getPostParams(String preString, Object object) {
        String result = getString(preString, "{");
        boolean isFirst = true;
        // 获取object对象对应类中的所有属性域
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            // 对于每个属性，获取属性名
            String varName = field.getName();
            try {
                // 获取原来的访问控制权限
                boolean accessFlag = field.isAccessible();
                // 修改访问控制权限
                field.setAccessible(true);
                // 获取在对象object中属性field对应的对象中的变量
                Object value = field.get(object);
                // 生成参数,其实跟get的URL中'?'后的参数字符串一致
                if (isFirst) {
                    if (value instanceof String) {
                        result += getString("\"", URLEncoder.encode(varName, "utf-8"), "\":\"",
                                URLEncoder.encode(String.valueOf(value), "utf-8"), "\"");
                    } else {
                        result += getString("\"", URLEncoder.encode(varName, "utf-8"), "\":",
                                URLEncoder.encode(String.valueOf(value), "utf-8"));
                    }
                    isFirst = false;
                } else {
                    if (value instanceof String) {
                        result += getString(",\"", URLEncoder.encode(varName, "utf-8"), "\":\"",
                                URLEncoder.encode(String.valueOf(value), "utf-8"), "\"");
                    } else {
                        result += getString(",\"", URLEncoder.encode(varName, "utf-8"), "\":",
                                URLEncoder.encode(String.valueOf(value), "utf-8"));
                    }
                }
                // 恢复访问控制权限
                field.setAccessible(accessFlag);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        result += "}";
        return result;
    }

    /**
     * 获取post请求中的参数
     */
    public static String getSimplePostParams(Object object) {
        String result = "";
        boolean isFirst = true;
        // 获取object对象对应类中的所有属性域
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            // 对于每个属性，获取属性名
            String varName = field.getName();
            try {
                // 获取原来的访问控制权限
                boolean accessFlag = field.isAccessible();
                // 修改访问控制权限
                field.setAccessible(true);
                // 获取在对象object中属性field对应的对象中的变量
                Object value = field.get(object);
                // 生成参数,其实跟get的URL中'?'后的参数字符串一致
                if (value != null) {
                    if (isFirst) {
                        result += getString(URLEncoder.encode(varName, "utf-8"), "=", URLEncoder.encode(String.valueOf(value), "utf-8"));
                        isFirst = false;
                    } else {
                        result +=
                                getString("&", URLEncoder.encode(varName, "utf-8"), "=", URLEncoder.encode(String.valueOf(value), "utf-8"));
                    }
                }
                // 恢复访问控制权限
                field.setAccessible(accessFlag);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 使用sha加密
     */
    public static String getSHA(String val) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md5.update(val.getBytes());
        byte[] m = md5.digest();//加密  
        return getString(m);
    }

    /**
     * 手机号合法性校验
     */
    public static boolean checkPhoneNumber(String value) {
//        String regExp = "^((13[0-9])|(15[^4,\\D])|(18[0-9])|(147))\\d{8}$";
        String regExp = "^1\\d{10}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(value);
        return m.find();
    }

    /**
     * 正则检测
     *
     * @param content
     * @param format
     * @return
     */
    public static boolean checkStringFormat(String content, String format) {
        Pattern p = Pattern.compile(format);
        Matcher m = p.matcher(content);
        return m.find();
    }

    public static boolean isNum(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
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

    /**
     * 获取该输入流的MD5值
     *
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String getMD5(InputStream is) throws NoSuchAlgorithmException, IOException {
        StringBuffer md5 = new StringBuffer();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] dataBytes = new byte[1024];

        int read;
        while ((read = is.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, read);
        }
        byte[] bytes = md.digest();

        // convert the byte to hex format
        for (byte b : bytes) {
            md5.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return md5.toString();
    }

    public static String getPrice(int price) {
        DecimalFormat format = new DecimalFormat("###0.00");
        float money = price / 100.0f;
        return format.format(money);
    }

    public static boolean isValidEnglish(String name) {
        String noSpaceName = name.replace(" ", "");
        String reg = "^[A-Za-z]+$";
        Matcher m = Pattern.compile(reg).matcher(noSpaceName);
        return m.find();
    }

    /**
     * 实现文本复制功能
     */
    public static void copy(Context context, String content) {
        ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(content.trim());
    }

    /**
     * 实现粘贴功能
     */
    public static String paste(Context context) {
        ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        return cmb.getText().toString().trim();
    }


    public static String dateFormat(long timeStamp, String dateFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.CHINA);
        Date date = new Date(timeStamp * 1000);
        return sdf.format(date);
    }

    /**
     * 部分编码后的url
     * @param url
     * @return
     */
    public static String restoreEncodeUrl(String url) {
        //修正反斜杠为斜杠
        url = url.replace("\\", "/");
        //使用长文本代替要保留字符串
        url = url.replace(":", "_*colon*_")
                .replace("/", "_*slash*_")
                .replace("\\", "_*backslash*_")
                .replace(" ", "_*blank*_")
                .replace("?", "_*question*_")
                .replace("=", "_*equal*_")
                .replace(";", "_*semicolon*_");

        //进行编码
        try {
            url = URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        url = url.replace("_*colon*_", ":")
                .replace("_*slash*_", "/")
                .replace("_*backslash*_", "\\")
                .replace("_*blank*_", "%20")
                .replace("_*question*_", "?")
                .replace("_*equal*_", "=")
                .replace("_*semicolon*_", ";");

        return url;
    }

    /**
     * spannable str
     *
     * @param context
     * @param fullStr
     * @param highLightStr
     * @param colorId
     * @return
     */
    public static SpannableStringBuilder getSpannable(Context context, String fullStr, String highLightStr, int colorId, int textSizeId) {
        int mStart;
        int mEnd;
        if (!TextUtils.isEmpty(fullStr) && !TextUtils.isEmpty(highLightStr)) {
            if (fullStr.contains(highLightStr)) {
                /*
                 *  返回highlightStr字符串wholeStr字符串中第一次出现处的索引。
				 */
                mStart = fullStr.indexOf(highLightStr);
                mEnd = mStart + highLightStr.length();
            } else {
                return new SpannableStringBuilder(fullStr);
            }
        } else {
            return new SpannableStringBuilder(fullStr);
        }
        SpannableStringBuilder spBuilder = new SpannableStringBuilder(fullStr);
        int color = ContextCompat.getColor(context, colorId);
        CharacterStyle charaStyle = new ForegroundColorSpan(color);//颜色
        spBuilder.setSpan(charaStyle, mStart, mEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spBuilder.setSpan(new AbsoluteSizeSpan(textSizeId), mStart, mEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spBuilder;
    }

    /**
     * 大小不一样的String
     * @param context
     * @param fullStr
     * @param highLightStr
     * @return
     */
    public static SpannableStringBuilder getTextSizeSpannable(Context context, String fullStr, String highLightStr, int textSizeId, int colorId) {
        int mStart;
        int mEnd;
        if (!TextUtils.isEmpty(fullStr) && !TextUtils.isEmpty(highLightStr)) {
            if (fullStr.contains(highLightStr)) {
                /*
				 *  返回highlightStr字符串wholeStr字符串中第一次出现处的索引。
				 */
                mStart = fullStr.indexOf(highLightStr);
                mEnd = mStart + highLightStr.length();
            } else {
                return new SpannableStringBuilder(fullStr);
            }
        } else {
            return new SpannableStringBuilder(fullStr);
        }
        SpannableStringBuilder spBuilder = new SpannableStringBuilder(fullStr);
        spBuilder.setSpan(new AbsoluteSizeSpan(textSizeId), mStart, mEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int color = ContextCompat.getColor(context, colorId);
        CharacterStyle charaStyle = new ForegroundColorSpan(color);//颜色
        spBuilder.setSpan(charaStyle, 0, fullStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spBuilder;
    }
}
