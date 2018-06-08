package io.github.wzj.music.ui.folder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/3/16
 * Time: 7:22 PM
 * Desc: FolderAdapter
 */
public class FolderAdapter extends BaseAdapter {

    private Context mContext;

    private List<FileDetail> mData;

    private int selection;

    public FolderAdapter(Context context, List<FileDetail> data) {
        mContext = context;
        mData = data;
    }

    public void setSelection(int i){
        this.selection = i;
    }

    public void setSelection(View view, int i){
        if(lastActiveView != null){
            lastActiveView.setActivated(false);
        }
        this.selection = i;
        view.setActivated(true);
        lastActiveView = view;
    }

    public void setData(List<FileDetail> data){
        this.mData = data;
    }

    @Override
    public int getCount() {
        return mData==null?0:mData.size();
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemViewType(int position) {
        FileDetail detail = mData.get(position);
        return detail.isFile()?0:1;
    }

    /**
     * 返回Item Type的总数量
     * */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    private View lastActiveView = null;

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        FileDetail detail = mData.get(i);
        if(view == null){
            view = new FolderItemView(detail, mContext);
        }
        if(view.isActivated()){
            view.setActivated(false);
        }

        ((FolderItemView)view).bind(detail);
        if(i == selection) {
            if(lastActiveView != null){
                lastActiveView.setActivated(false);
            }
            view.setActivated(true);
            lastActiveView = view;
        }
        return view;
    }
}
