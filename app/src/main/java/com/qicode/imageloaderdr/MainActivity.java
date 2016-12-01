package com.qicode.imageloaderdr;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.qicode.imageloaderdr.adapter.UniversalAdapter;
import com.qicode.imageloaderdr.adapter.UniversalViewHolder;
import com.qicode.imageloaderdr.imageloader.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ImageLoader mImageLoader;

    RecyclerView mRcv;
    List<String> mUrls = new ArrayList<>();
    String[] mUrlArr = {
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/000bb9836ea21a9e5989d59387477e83.JPG",
            "http://art-sign-pro.oss-cn-beijing.aliyuncs.com/image/expert_sign/003b5b828e24f3e01dd832c725fc6da0.JPG"

    };
    UniversalAdapter<String> mAdapter = new UniversalAdapter<>(mUrls, new UniversalAdapter.OnBindDataInterface<String>() {
        @Override
        public void onBindData(String model, UniversalViewHolder holder, int pos, int type) {
            ImageView iv = holder.getSubView(R.id.img);
            mImageLoader.loadImage(model, iv, true);
        }

        @Override
        public int getItemLayoutId(int viewType) {
            return R.layout.item_img;
        }
    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageLoader = ImageLoader.getInstance(this);

        for(int i = 0; i < mUrlArr.length; i++){
            mUrls.add(mUrlArr[i]);
        }
        mRcv = (RecyclerView) findViewById(R.id.rcv);
        mRcv.setLayoutManager(new GridLayoutManager(this, 1));
        mRcv.setAdapter(mAdapter);

    }
}
