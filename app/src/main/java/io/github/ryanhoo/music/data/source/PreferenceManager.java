package io.github.ryanhoo.music.data.source;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import io.github.ryanhoo.music.data.model.PlayList;
import io.github.ryanhoo.music.data.model.Song;
import io.github.ryanhoo.music.player.PlayMode;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/10/16
 * Time: 11:05 PM
 * Desc: PreferenceManager
 */
public class PreferenceManager {

    private static final String PREFS_NAME = "config.xml";

    /**
     * For deciding whether to add the default folders(SDCard/Download/Music),
     * if it's being deleted manually by users, then they should not be auto-recreated.
     * {@link #isFirstQueryFolders(Context)}, {@link #reportFirstQueryFolders(Context)}
     */
    private static final String KEY_FOLDERS_FIRST_QUERY = "firstQueryFolders";

    /**
     * Play mode from the last time.
     */
    private static final String KEY_PLAY_MODE = "playMode";

    private static final String KEY_FOLDER = "playFoler";

    private static final String KEY_PATH = "playPath";

    private static final String KEY_PAGE = "page";

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor edit(Context context) {
        return preferences(context).edit();
    }

    /**
     * {@link #KEY_FOLDERS_FIRST_QUERY}
     */
    public static boolean isFirstQueryFolders(Context context) {
        return preferences(context).getBoolean(KEY_FOLDERS_FIRST_QUERY, true);
    }

    /**
     * {@link #KEY_FOLDERS_FIRST_QUERY}
     */
    public static void reportFirstQueryFolders(Context context) {
        edit(context).putBoolean(KEY_FOLDERS_FIRST_QUERY, false).commit();
    }

    /**
     * {@link #KEY_PLAY_MODE}
     */
    public static PlayMode lastPlayMode(Context context) {
        String playModeName = preferences(context).getString(KEY_PLAY_MODE, null);
        if (playModeName != null) {
            return PlayMode.valueOf(playModeName);
        }
        return PlayMode.getDefault();
    }

    /**
     * {@link #KEY_PLAY_MODE}
     */
    public static void setPlayMode(Context context, PlayMode playMode) {
        edit(context).putString(KEY_PLAY_MODE, playMode.name()).commit();
    }

    public static void setPlayFolderAndPath(Context context, String folder, String path){
        if(folder == null && path == null){
            return;
        }else if(folder != null && path != null){
            SharedPreferences.Editor edit = edit(context);
            edit.putString(KEY_FOLDER, folder);
            edit.putString(KEY_PATH, path);
            edit.commit();
        }else if(folder != null){
            SharedPreferences.Editor edit = edit(context);
            edit.putString(KEY_FOLDER, folder);
            edit.commit();
        }else{
            SharedPreferences.Editor edit = edit(context);
            edit.putString(KEY_PATH, path);
            edit.commit();
        }
    }

    public static class PlayFolder{
        public String folder;
        public String path;
    }

    public static PlayFolder getPlayFolder(Context context){
        SharedPreferences preferences = preferences(context);
        String folder = preferences.getString(KEY_FOLDER, null);
        String path = preferences.getString(KEY_PATH, null);
        PlayFolder playFolder = new PlayFolder();
        playFolder.folder = folder;
        playFolder.path = path;
        return playFolder;
    }

    public static PlayList getPlayList(Context context) {
        PlayFolder playFolder = getPlayFolder(context);
        if (playFolder.folder == null) {
            return null;
        }

        File rootFile = new File(playFolder.folder);
        if (rootFile.isDirectory()) {
            File[] mp3Files = rootFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return !file.getName().startsWith(".") && file.isFile() && file.getName().endsWith(".mp3");
                }
            });
            Log.e("wzj", "mp3 count: " + mp3Files.length);
            Arrays.sort(mp3Files, new Comparator<File>() {
                @Override
                public int compare(File fileDetail, File t1) {
                    return fileDetail.getName().compareTo(t1.getName());
                }
            });
            PlayList playList = new PlayList();
            ArrayList<Song> songs = new ArrayList<>(mp3Files.length);
            int playIndex = 0;
            for (int i = 0; i < mp3Files.length; i++) {
                Song song = new Song();
                song.setDuration(-1);
                song.setPath(mp3Files[i].getAbsolutePath());
                //songs.add(FileUtils.fileToMusic(mp3Files[i]));
                songs.add(song);
                if (mp3Files[i].getAbsolutePath().equals(playFolder.path)) {
                    playIndex = i;
                    Log.e("wzj", "preference, play index: " + i);
                }
            }
            playList.setSongs(songs);
            playList.setPlayingIndex(playIndex);
            return playList;
        } else {
            return null;
        }
    }

}
