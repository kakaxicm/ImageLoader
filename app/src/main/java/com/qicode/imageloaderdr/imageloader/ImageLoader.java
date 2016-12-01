package com.qicode.imageloaderdr.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;

import com.qicode.imageloaderdr.util.BitmapUtils;
import com.qicode.imageloaderdr.util.ImageSize;
import com.qicode.imageloaderdr.util.ImageSizeUtil;
import com.qicode.imageloaderdr.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by chenming on 16/10/3.
 */
public class ImageLoader {
    /**
     * ImageLoader结构
     * 1.LRU内存缓存
     * 2.后台线程,用于调度下载图片的线程,通过构建后台消息模型及信号量实现并发下载
     * 3.下载线程池
     * 4.磁盘缓存
     * 5.更新ImageView的handler
     */
    private static ImageLoader mInstance;

    /**
     * 线程池
     */
    private ExecutorService mThreadPool;

    /**
     * 后台轮询线程及Handler,信号量
     */
    private HandlerThread mBackLoopThread;//后台线程

    private class BackLoopHandler extends Handler {

        BackLoopHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //以下代码运行在子线程,采用信号量来调度任务
            //任务入队
            mThreadPool.execute(getTask());
            //同时间的下载任务个数信号量同步,执行任务到达上限,则阻塞
            try {
                mBackLoopThreadSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private BackLoopHandler mBackLoopThreadHandler;//发消息给后台Looper，调度下载线程
    private Semaphore mBackLoopThreadSemaphore;//后台下载任务个数限制的信号量,控制同时下载的数量

    private LinkedList<Runnable> mTaskQueue;//所有任务队列
    private Type mType = Type.LIFO;//调度方式,默认后进先出

    //缓存配置
    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;
    private boolean mIsDiskCacheEnable = true;//磁盘缓存开关,默认开启

    //更新ui
    private Handler mUIHandler;

    public static ImageLoader getInstance(Context context) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(context, 10, Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    /**
     * @param threadCount 同时下载图片线程个数
     * @param type        调度策略
     */
    private ImageLoader(Context context, int threadCount, Type type) {
        init(context, threadCount, type);
    }

    private void init(Context context, int threadCount, Type type) {
        initTaskDispatchThread();
        mBackLoopThreadSemaphore = new Semaphore(threadCount);//并发数量控制信号量
        // 创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList();
        mType = type;

        //缓存配置
        initMemoryCache();
        initDiskCache(context);


    }

    /**
     * 内存缓存
     */
    private void initMemoryCache() {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    /**
     * 磁盘缓存初始化
     *
     * @param context
     */
    private void initDiskCache(Context context) {
        //磁盘缓存初始化
        String dir = getImageCacheFile(context);
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
        File diskCacheDir = new File(dir);
        try {
            mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * DiskLru缓存目录
     *
     * @param context
     * @return
     */
    private String getImageCacheFile(Context context) {
        return StringUtils
                .getString(context.getCacheDir(), "/cacheImage/");
    }

    /**
     * 初始化后台调度线程,结合信号量及调度策略,实现并发下载
     */
    private void initTaskDispatchThread() {
        //后台轮询线程初始化HandlerThread
        mBackLoopThread = new HandlerThread("backthread");
        mBackLoopThread.start();
        mBackLoopThreadHandler = new BackLoopHandler(mBackLoopThread.getLooper());
    }

    /**
     * 根据调度策略取任务
     *
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        }

        return mTaskQueue.removeLast();
    }

    /**
     * 新建任务，添加到后台任务队列
     *
     * @param runnable
     */
    public synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        mBackLoopThreadHandler.sendEmptyMessage(0x110);//给后台调度Looper发消息,
    }

    /**
     * 调用入口
     *
     * @param path      文件或者网络地址
     * @param imageView 目标控件
     * @param isFromNet true 本地图片 false 加载本地图片
     */
    public void loadImage(final String path, final ImageView imageView, final boolean isFromNet) {
        imageView.setTag(path);//避免错位
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                public void handleMessage(Message msg) {
                    // 获取得到图片，为imageview回调设置图片
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageview = holder.imageView;
                    String path = holder.path;
                    // 将path与getTag存储路径进行比较
                    if (imageview.getTag().toString().equals(path)) {
                        imageview.setImageBitmap(bm);
                    }
                }
            };
        }

        //内存缓存检测
        Bitmap bp = getBitmapFromLruCache(BitmapUtils.toMD5(path));
        if (bp == null) {
            addTask(buildTask(path, imageView, isFromNet));
        } else {
            refreshBitmap(path, imageView, bp);
        }
    }

    /**
     * 获取bp的核心代码,没有内存缓存时,下载网络图片，加入磁盘和内存缓存
     *
     * @return
     */
    private Runnable buildTask(final String path, final ImageView imageView, final boolean isFromNet) {
        return new Runnable() {
            @Override
            public void run() {
                Bitmap bp = null;
                if (isFromNet) {//加载网络图片
                    if (mIsDiskCacheEnable) {
                        //本地缓存处理
                        //disklrucache读缓存
                        try {
                            bp = getBitmapFromDisk(path);
                            if (bp == null) {
                                //下载图片
                                bp = ImageDownloader.downloadImgFromUrl(path, imageView);
                                if (bp != null) {
                                    saveBitmapToDisk(path, bp);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //直接从网络加载
                        bp = ImageDownloader.downloadImgFromUrl(path, imageView);
                    }
                } else {
                    //加载本地图片
                    bp = loadImageFromLocal(path, imageView);
                }

                mBackLoopThreadSemaphore.release();//该任务执行完成, 释放并发加载图片信号量
                if (bp != null) {
                    //更新UI
                    refreshBitmap(path, imageView, bp);
                    //加入内存缓存
                    addBitmapToLruCache(BitmapUtils.toMD5(path), bp);
                }
            }
        };
    }

    /**
     * 写入bitmap到磁盘
     *
     * @param path
     * @param bp
     */
    private void saveBitmapToDisk(String path, Bitmap bp) throws IOException {
        boolean isFinish = false;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        InputStream in = new ByteArrayInputStream(baos.toByteArray());
        OutputStream os = null;
        DiskLruCache.Editor editor = null;
        String key = StringUtils.toMD5(path);
        editor = mDiskLruCache.edit(key);
        if (editor != null) {
            os = editor.newOutputStream(0);
        }

        int rbyte;
        if (os != null) {
            while ((rbyte = in.read()) != -1) {
                os.write(rbyte);
            }
            isFinish = true;
        }
        //提交
        if (editor != null) {
            if (isFinish) {
                editor.commit();

            } else {
                editor.abort();
            }
        }
        mDiskLruCache.flush();

    }

    /**
     * 从磁盘缓存中取bp
     *
     * @param path
     * @return
     * @throws IOException
     */
    private Bitmap getBitmapFromDisk(String path) throws IOException {
        Bitmap bp = null;
        String key = StringUtils.toMD5(path);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if (snapshot != null) {
            FileInputStream fis = (FileInputStream) snapshot.getInputStream(0);
            bp = BitmapFactory.decodeStream(fis);
        }
        return bp;
    }

    /**
     * 更新UI
     *
     * @param path
     * @param imageView
     * @param bm
     */
    private void refreshBitmap(final String path, final ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    public enum Type {
        FIFO, LIFO;
    }

    private class ImgBeanHolder {
        private Bitmap bitmap;
        private ImageView imageView;
        private String path;
    }

    /**
     * 加载本地图片
     *
     * @param path
     * @param imageView
     * @return
     */
    private Bitmap loadImageFromLocal(final String path, final ImageView imageView) {
        Bitmap bm;
        // 加载图片
        // 图片的压缩
        // 1、获得图片需要显示的大小
        ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
        // 2、压缩图片
        bm = decodeSampledBitmapFromPath(path, imageSize.width,
                imageSize.height);
        return bm;
    }

    /**
     * 压缩本地图片
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
        opts.inSampleSize = ImageSizeUtil.caculateInSampleSize(opts, width, height);
        opts.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(path, opts);
        return bitmap;
    }

    private Bitmap getBitmapFromLruCache(String path) {
        Bitmap bp = mLruCache.get(path);
        return bp;
    }

    /**
     * 将图片加入LruCache
     *
     * @param path
     * @param bm
     */
    protected void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null)
                mLruCache.put(path, bm);
        }
    }

    public void quit() {
        mBackLoopThread.quit();
    }
}
