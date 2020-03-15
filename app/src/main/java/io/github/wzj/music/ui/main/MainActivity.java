package io.github.wzj.music.ui.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import io.github.wzj.music.R;
import io.github.ryanhoo.music.ui.base.BaseActivity;
import io.github.ryanhoo.music.ui.base.BaseFragment;
import io.github.wzj.music.ui.folder.FolderFragment;
import io.github.wzj.music.ui.music.MusicPlayerFragment;
import io.github.wzj.music.ui.settings.SettingsFragment;
import io.github.ryanhoo.music.ui.widget.NoScrollViewPager;

import java.util.List;

public class MainActivity extends BaseActivity {

    static final int DEFAULT_PAGE_INDEX = 1;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.view_pager)
    NoScrollViewPager viewPager;
    @BindViews({R.id.radio_button_local_files, R.id.radio_button_music, R.id.radio_button_settings})
    List<RadioButton> radioButtons;

    String[] mTitles;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static boolean hasPermission(Context context, String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        for (String permission : permissions) {
            return hasPermission(context, permission);
        }
        return true;
    }

    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
            return false;
        return true;
    }

    public static void requestPermission(Activity activity, String[] permissions, int request) {
        ActivityCompat.requestPermissions(activity, permissions, request);
    }

    private boolean checkStoragePermission() {
        String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (!hasPermission(this, permissions)) {
            requestPermission(this, permissions, 1000);
            return false;
        } else {
            //granted .. to do something
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted.. to do something
                    init();
                } else {
                    System.exit(-1);
                    //shouldShowRequestPermissionRationale 拒绝时是否勾选了不再提示
                    // 没有勾选返回true ,勾选不再提醒返回false
                    boolean hasAlwaysDeniedPermission = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    //勾选不再提醒,会过滤系统的对话框，这时需要手动去设置权限
                    if (hasAlwaysDeniedPermission) {

                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CoordinatorLayout x;
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // Main Controls' Titles
        mTitles = getResources().getStringArray(R.array.mp_main_titles);
        if(checkStoragePermission()) {
            init();
        }

    }
    private Handler mHandler = new Handler();
    private void init() {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }, 30*60*1000);
        // Fragments
        BaseFragment[] fragments = new BaseFragment[mTitles.length];
        //fragments[0] = new PlayListFragment();
        fragments[0] = new FolderFragment();
        fragments[1] = new MusicPlayerFragment();
        fragments[2] = new SettingsFragment();

        // Inflate ViewPager
        MainPagerAdapter adapter = new MainPagerAdapter(getSupportFragmentManager(), mTitles, fragments);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getCount() - 1);
        viewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.mp_margin_large));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Empty
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Empty
            }

            @Override
            public void onPageSelected(int position) {
                radioButtons.get(position).setChecked(true);
            }
        });

//        viewPager.setOnTouchListener(new View.OnTouchListener(){
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return true;
//            }
//        });


        radioButtons.get(DEFAULT_PAGE_INDEX).setChecked(true);

//        LiteOrm orm = LiteOrmHelper.getInstance();
//        SentenceSplitPoint breakPoint = new SentenceSplitPoint();
//        breakPoint.setPath("/home/afafagafaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
//        String point = "";
//        for(int i=0; i<100; i++){
//            point += "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
//        }
//        Log.e("wzj", "point len: " + point.length());
//        breakPoint.setPoints(point);
//        orm.save(breakPoint);
//        SentenceSplitPoint p2 = orm.queryById(breakPoint.getPath(), SentenceSplitPoint.class);
//        Log.e("wzj", "point 2 len: " + p2.getPoints().length());
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @OnCheckedChanged({R.id.radio_button_local_files, R.id.radio_button_music, R.id.radio_button_settings})
    public void onRadioButtonChecked(RadioButton button, boolean isChecked) {
        if (isChecked) {
            onItemChecked(radioButtons.indexOf(button));
        }
    }

    private void onItemChecked(int position) {
        viewPager.setCurrentItem(position);
        toolbar.setTitle(mTitles[position]);
    }

    public void gotoMusicFragment(){
        onItemChecked(1);
    }
}
