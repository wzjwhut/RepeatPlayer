package io.github.wzj.music.ui.main;

import android.os.Bundle;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CoordinatorLayout x;
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // Main Controls' Titles
        mTitles = getResources().getStringArray(R.array.mp_main_titles);

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
