package com.qicode.imageloaderdr.adapter;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.qicode.imageloaderdr.util.StringUtils;


/**
 * Created by star on 16/7/6.
 *
 */
public class UniversalViewHolder extends RecyclerView.ViewHolder{
    private SparseArray<View> mViews;
    private View mContentView;

    public static UniversalViewHolder getViewHolder(ViewGroup parent, int layoutId)
    {
        return new UniversalViewHolder(View.inflate(parent.getContext(), layoutId, null));
    }

    public UniversalViewHolder(View itemView) {
        super(itemView);
        mViews = new SparseArray<>();
        mContentView = itemView;
    }

    public <T extends View> T getSubView(int viewId){
        View view = mViews.get(viewId);
        if(view == null){
            view = mContentView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (T)view;
    }

    public View getContentView(){
        return mContentView;
    }

    /**
     * 辅助方法 设置文本
     * @param id
     * @param s
     */
    public void setText(int id, String s){
        TextView tv = getSubView(id);
        tv.setText(s);
    }

    /**
     * 设置spanner
     * @param id
     * @param spanner
     */
    public void setText(int id, SpannableStringBuilder spanner){
        TextView tv = getSubView(id);
        tv.setText(spanner);
    }

    /**
     * 字体颜色
     * @param id
     * @param color
     */
    public void setTextColor(int id, int color){
        TextView tv = getSubView(id);
        tv.setTextColor(color);
    }

    /**
     * 设置背景颜色
     * @param id
     * @param color
     */
    public void setBackGroundColor(int id, int color){
        View view = getSubView(id);
        view.setBackgroundColor(color);
    }

    /**
     * 设置背景图片
     * @param id
     * @param drawaleResId
     */
    public void setBackGroundDrawable(int id, int drawaleResId){
        View view = getSubView(id);
        view.setBackgroundResource(drawaleResId);
    }

    /**
     * 设置图片
     * @param id
     * @param imageId
     */
    public void setImage(int id, int imageId){
        ImageView img = getSubView(id);
        img.setImageResource(imageId);
    }

    /**
     * 辅助方法 设置sdv地址
     * @param id
     * @param url
     */
    public void setSimpleDraweeView(int id, String url){
        SimpleDraweeView sdv = getSubView(id);
        url = StringUtils.restoreEncodeUrl(url);
        sdv.setImageURI(Uri.parse(url));
    }

    /**
     * 辅助方法 设置sdv地址
     * @param id
     * @param url
     * @param ratio
     */
    public void setSimpleDraweeView(int id, String url, float ratio){
        SimpleDraweeView sdv = getSubView(id);
        sdv.setAspectRatio(ratio);
        url = StringUtils.restoreEncodeUrl(url);
        sdv.setImageURI(Uri.parse(url));
    }

    /**
     * 绑定单击事件
     * @param id
     * @param tag
     * @param listener
     */
    public void setOnclickListener(int id, Object tag, View.OnClickListener listener){
        View view = getSubView(id);
        view.setTag(tag);
        view.setOnClickListener(listener);
    }

    /**
     * select view
     * @param id
     * @param b
     */
    public void setViewSelected(int id, boolean b){
        getSubView(id).setSelected(b);
    }
}
