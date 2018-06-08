package io.github.ryanhoo.music.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/3/16
 * Time: 7:19 PM
 * Desc: Folder
 */
@Table("folder")
public class SentenceSplitPoint implements Parcelable {

    public static final String COLUMN_NAME = "name";

    @PrimaryKey(AssignType.BY_MYSELF)
    private String path;

    private String points;

    private long filetime;

    public SentenceSplitPoint() {
        // Empty
    }

    public SentenceSplitPoint(Parcel in) {
        readFromParcel(in);
    }

    public ArrayList<Integer> getBreakPoints(){
        try {
            String[] splits = StringUtils.split(points, ',');
            ArrayList<Integer> list = new ArrayList<>(splits.length);
            for(String str : splits){
                list.add(Integer.parseInt(str));
            }
            return list;
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public long getFiletime() {
        return filetime;
    }

    public void setFiletime(long filetime) {
        this.filetime = filetime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.path);
        dest.writeString(this.points);
        dest.writeLong(this.filetime);
    }

    private void readFromParcel(Parcel in) {
        this.path = in.readString();
        this.points = in.readString();
        this.filetime = in.readLong();
    }

    public static final Creator<SentenceSplitPoint> CREATOR = new Creator<SentenceSplitPoint>() {
        @Override
        public SentenceSplitPoint createFromParcel(Parcel source) {
            return new SentenceSplitPoint(source);
        }

        @Override
        public SentenceSplitPoint[] newArray(int size) {
            return new SentenceSplitPoint[size];
        }
    };
}
