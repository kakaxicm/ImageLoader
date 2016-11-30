package com.qicode.imageloaderdr.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.qicode.imageloaderdr.util.BitmapUtils;
import com.qicode.imageloaderdr.util.ImageSize;
import com.qicode.imageloaderdr.util.ImageSizeUtil;

import java.io.File;
import java.io.IOException;
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
    private Thread mBackLoopThread;//后台线程

    private Handler mBackLoopThreadHandler;//发消息给后台Looper，调度下载线程
    private Semaphore mBackLoopThreadSemaphore;//后台下载任务个数限制的信号量,控制同时下载的数量

    private Semaphore mBackLoopThreadInitSemaphore = new Semaphore(0);//用于同步,保证后台线程初始化完成

    private LinkedList<Runnable> mTaskQueue;//所有任务队列
    private Type mType = Type.LIFO;//调度方式,默认后进先出

    private LruCache<String, Bitmap> mLruCache;
    private boolean mIsDiskCacheEnable = true;//磁盘缓存开关

    private Handler mUIHandler;

    public static ImageLoader getInstance(int concurrentCount, Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(concurrentCount, type);
                }
            }
        }
        return mInstance;
    }

    /**
     * @param threadCount 同时下载图片线程个数
     * @param type        调度策略
     */
    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        initTaskDispatchThread();
        mBackLoopThreadSemaphore = new Semaphore(threadCount);
        // 创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList();
        mType = type;

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory/8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    /**
     * 初始化后台调度线程,结合信号量及调度策略,实现并发下载
     */
    private void initTaskDispatchThread() {
        //后台轮询线程初始化HandlerThread
        mBackLoopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mBackLoopThreadHandler = new Handler() {
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
                };
                mBackLoopThreadInitSemaphore.release();//后台轮询线程初始化成功,释放信号量
                Looper.loop();//开启消息循环
            }
        });
        mBackLoopThread.start();

    }

    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        }

        return mTaskQueue.removeLast();
    }

    /**
     * @param runnable
     */
    public synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mBackLoopThreadHandler == null)
                mBackLoopThreadInitSemaphore.acquire();//等待轮询线程初始化完毕
        } catch (InterruptedException e) {
        }
        mBackLoopThreadHandler.sendEmptyMessage(0x110);//给后台调度Looper发消息,
    }

    /**
     * 调用入口
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
        if(bp == null){
            Log.d("TAG", "loadImage:非内存缓存");
            addTask(buildTask(path, imageView, isFromNet));
        }else {
            Log.d("TAG", "loadImage:内存缓存");
            refreshBitmap(path, imageView, bp);
        }
    }

    /**
     * 核心代码,没有内存缓存时,下载网络图片，加入磁盘和内存缓存
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
                        //查看缓存
                        String cacheFileName = BitmapUtils.toMD5(path);
                        String cacheFileAbsPath = BitmapUtils.getTempSaveDir(imageView.getContext()) + cacheFileName;
                        File file = new File(cacheFileAbsPath);
                        if (file.exists()) {
                            //如果存在 则加载本地图片
                            bp = loadImageFromLocal(cacheFileAbsPath, imageView);
                            Log.d("TAG", "读取本地缓存:" + " " + (bp != null)  + cacheFileAbsPath);
                        } else {
                            //否则下载图片
                            //创建文件
                            try {
                                file.createNewFile();
                            } catch (IOException e) {
                            }
                            Log.d("TAG", "开始下载任务:" + path);
                            bp = ImageDownloader.downloadImgFromUrl(path, imageView);
                            if (bp != null) {
                                //写磁盘缓存
                                Log.d("TAG", "下载成功" + path);
                                BitmapUtils.saveBitmap(imageView.getContext(), bp, cacheFileName);
                            }
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
                    Log.d("TAG", "refreshBitmap:非内存缓存");
                    refreshBitmap(path, imageView, bp);
                    //加入内存缓存
                    addBitmapToLruCache(BitmapUtils.toMD5(path), bp);
                }
            }
        };
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

    private Bitmap getBitmapFromLruCache(String path){
        Bitmap bp = mLruCache.get(path);
        return bp;
    }

    /**
     * 将图片加入LruCache
     *
     * @param path
     * @param bm
     */
    protected void addBitmapToLruCache(String path, Bitmap bm)
    {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null)
                mLruCache.put(path, bm);
        }
    }
}
