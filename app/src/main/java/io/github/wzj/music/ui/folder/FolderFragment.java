package io.github.wzj.music.ui.folder;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.ryanhoo.music.RxBus;
import io.github.ryanhoo.music.ui.music.MusicPlayerContract;
import io.github.ryanhoo.music.ui.music.MusicPlayerPresenter;
import io.github.wzj.music.R;
import io.github.ryanhoo.music.data.model.PlayList;
import io.github.ryanhoo.music.data.model.Song;
import io.github.ryanhoo.music.data.source.PreferenceManager;
import io.github.ryanhoo.music.event.PlayListNowEvent;
import io.github.ryanhoo.music.player.IPlayback;
import io.github.ryanhoo.music.player.PlayMode;
import io.github.ryanhoo.music.player.PlaybackService;
import io.github.ryanhoo.music.player.Player;
import io.github.ryanhoo.music.ui.base.BaseFragment;
import io.github.ryanhoo.music.ui.music.MusicPlayerContract;
import io.github.ryanhoo.music.ui.music.MusicPlayerPresenter;
import io.github.wzj.music.ui.main.MainActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/3/16
 * Time: 7:29 PM
 * Desc: FolderFragment
 */
public class FolderFragment extends BaseFragment  implements MusicPlayerContract.View, IPlayback.Callback {
    private final static Logger logger = LogManager.getLogger(FolderFragment.class);
    ListView recyclerView;
    private TextView backView;

    private FolderAdapter mAdapter;
    private int mUpdateIndex, mDeleteIndex;

    private String currentPath;
    private String rootPath;

    private IPlayback mPlayer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_added_folders, container, false);
    }

    public String getSDPath(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if(sdCardExist)
        {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }

    private List<FileDetail> getFileList(String path){
        List<FileDetail> folders;
        File rootFile = new File(path);
        if(rootFile.isDirectory()){
            File[] files = rootFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return !file.getName().startsWith(".") && (file.isDirectory() || file.getName().endsWith(".mp3"));
                }
            });
            folders = new ArrayList<>(files.length);
            Log.e("wzj", "folder count: " + files.length);
            //new Exception().printStackTrace();
            for(File file : files){
                FileDetail detail = new FileDetail();
                detail.setFile(file.isFile());
                detail.setName(file.getName());
                detail.setPath(file.getAbsolutePath());
                if(file.isDirectory()){
                    detail.setNumOfSongs(file.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.getName().endsWith(".mp3");
                        }
                    }).length);
                }
                folders.add(detail);
                Collections.sort(folders, new Comparator<FileDetail>() {
                    @Override
                    public int compare(FileDetail fileDetail, FileDetail t1) {
                        return fileDetail.getName().compareTo(t1.getName());
                    }
                });
            }
        }else{
            folders = new LinkedList<>();
            Log.e("wzj", "folder not directory");
        }
        return folders;
    }
    private List<FileDetail> mFolders;
    private String mSelectedMp3File;

    private int getSelectionIndex(String path){
        int selection = 0;
        int index = 0;
        if(path != null){
            for(FileDetail detail : mFolders){
                if(detail.isFile() && detail.getPath().equals(path)){
                    selection = index;
                    break;
                }
                index++;
            }
        }
        return selection;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = (ListView) this.getView().findViewById(R.id.recycler_view);
        backView = (TextView)this.getView().findViewById(R.id.back);
        rootPath = getSDPath() + "/";
        PreferenceManager.PlayFolder folder = PreferenceManager.getPlayFolder(getActivity());
        if(folder.folder == null) {
            currentPath = rootPath;
        }else{
            currentPath = folder.folder;
        }

        mSelectedMp3File = folder.path;
        mFolders = getFileList(currentPath);
        int selection = getSelectionIndex(mSelectedMp3File);
        mAdapter = new FolderAdapter(getActivity(), mFolders);
        recyclerView.setAdapter(mAdapter);
        mAdapter.setSelection(selection);
        recyclerView.setSelection(selection);

        recyclerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                 FileDetail detail = (FileDetail) mAdapter.getItem(i);
                logger.error("clicked: {}", detail.getPath());
                if(detail.isFile()){
                    try {
                        //Toast.makeText(FolderFragment.this.getContext(), "mp3", Toast.LENGTH_SHORT).show();
                        mAdapter.setSelection(view, i);
                        view.setActivated(true);
                        mSelectedMp3File = detail.getPath();
                        PreferenceManager.setPlayFolderAndPath(FolderFragment.this.getContext(), currentPath, mSelectedMp3File);
                        PlayList playList = PreferenceManager.getPlayList(getContext());

                        PlayListNowEvent playListNowEvent = new PlayListNowEvent(playList, playList.getPlayingIndex());
                        logger.info("play list event");
                        RxBus.getInstance().post(playListNowEvent);
                        ((MainActivity)FolderFragment.this.getActivity()).gotoMusicFragment();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }else{
                    //Toast.makeText(FolderFragment.this.getContext(), "folder", Toast.LENGTH_SHORT).show();
                    currentPath = detail.getPath() ;
                    mFolders = getFileList(detail.getPath());
                    mAdapter.setData(mFolders);
                    mAdapter.notifyDataSetChanged();
                    PreferenceManager.setPlayFolderAndPath(FolderFragment.this.getContext(), currentPath, null);
                }
            }
        });

        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(rootPath.startsWith(currentPath)){
                    return;
                }
                currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
                List<FileDetail> folders = getFileList(currentPath);
                mAdapter.setData(folders);
                mAdapter.notifyDataSetChanged();
            }
        });

        new MusicPlayerPresenter(getActivity(), this).subscribe();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @Override
    public void setPresenter(MusicPlayerContract.Presenter presenter) {

    }

    @Override
    public void handleError(Throwable error) {

    }

    @Override
    public void onPlaybackServiceBound(PlaybackService service) {
        mPlayer = service;
        Player.getInstance().registerCallback(this);
        //mPlayer.registerCallback(this);
        Log.e("wzj", "[FolderFragement] onservice bound");
    }

    @Override
    public void onPlaybackServiceUnbound() {

    }

    @Override
    public void onSongSetAsFavorite(@NonNull Song song) {

    }

    @Override
    public void onSongUpdated(@Nullable Song song) {
        updateSong(song);
    }

    @Override
    public void updatePlayMode(PlayMode playMode) {

    }

    private void updateSong(Song song){
        if(song == null){
            Log.e("wzj", "song is null");
            return;
        }
        String path = song.getPath();
        Log.e("wzj", "song path " + path);
        if(!path.equals(mSelectedMp3File)){
            mSelectedMp3File = path;
            PreferenceManager.setPlayFolderAndPath(getContext(), null, mSelectedMp3File);
            String dir = FilenameUtils.getFullPath(path);
            Log.e("wzj", " root: " + currentPath);
            Log.e("wzj", " dir: " + dir);
            if(currentPath != null && Math.abs(dir.length() - currentPath.length())<=1){
                int selection = getSelectionIndex(mSelectedMp3File);
                Log.e("wzj", "set highlight: " + selection);
                mAdapter.setSelection(selection);
                recyclerView.setSelection(selection);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void updatePlayToggle(boolean play) {

    }

    @Override
    public void updateFavoriteToggle(boolean favorite) {

    }

    @Override
    public void onSwitchLast(@Nullable Song last) {
        updateSong(last);
    }

    @Override
    public void onSwitchNext(@Nullable Song next) {
        updateSong(next);
    }

    @Override
    public void onComplete(@Nullable Song next) {
        updateSong(next);
    }

    @Override
    public void onPlayStatusChanged(boolean isPlaying) {

    }
}
