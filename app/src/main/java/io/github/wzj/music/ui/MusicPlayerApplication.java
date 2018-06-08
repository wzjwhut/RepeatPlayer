package io.github.wzj.music.ui;

import android.app.Application;

import io.github.wzj.music.R;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;


public class MusicPlayerApplication extends Application {

    private static MusicPlayerApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        // Custom fonts
        CalligraphyConfig.initDefault(
                new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Roboto-Monospace-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

    }

    public static MusicPlayerApplication getInstance() {
        return sInstance;
    }
}
