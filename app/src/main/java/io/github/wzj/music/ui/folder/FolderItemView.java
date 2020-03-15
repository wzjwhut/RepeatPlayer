package io.github.wzj.music.ui.folder;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.wzj.music.R;
import io.github.ryanhoo.music.utils.TimeUtils;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/3/16
 * Time: 7:22 PM
 * Desc: FolderItemView
 */
public class FolderItemView extends RelativeLayout {

    @BindView(R.id.text_view_name)
    TextView textViewName;
    @BindView(R.id.text_view_info)
    TextView textViewInfo;
    @BindView(R.id.layout_action)
    View buttonAction;

    public FolderItemView(FileDetail folder, Context context) {
        super(context);
        View.inflate(context, folder.isFile()?R.layout.item_added_mp3:R.layout.item_added_folder, this);
        ButterKnife.bind(this);
    }

    public void bind(FileDetail folder){
        textViewName.setText(folder.getName());
        if(folder.isFile()){
            int duration = folder.getDuration();
            if(duration < 0){
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(folder.getPath());
                String keyDuration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                // ensure the duration is a digit, otherwise return null song
                if (keyDuration == null || !keyDuration.matches("\\d+")) {
                    duration = 0;
                }else {
                    duration = Integer.parseInt(keyDuration);
                }
                folder.setDuration(duration);
            }
            textViewInfo.setText(TimeUtils.formatDuration(duration));
        }else{
            if(folder.getNumOfSongs()>0) {
                textViewInfo.setText(getContext().getString(
                        R.string.mp_local_files_folder_list_item_info_formatter,
                        folder.getNumOfSongs()));
            }else{
                textViewInfo.setText("");
            }
        }
    }
}
