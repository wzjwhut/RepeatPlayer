package io.github.wzj.music.ui.folder;

/**
 * Created by Administrator on 2018/5/30/030.
 */

public class FileDetail {
    private String name;

    private String path;

    private int numOfSongs;

    private boolean isFile;

    private int duration = -1;

    public FileDetail() {
        // Empty
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getNumOfSongs() {
        return numOfSongs;
    }

    public void setNumOfSongs(int numOfSongs) {
        this.numOfSongs = numOfSongs;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
