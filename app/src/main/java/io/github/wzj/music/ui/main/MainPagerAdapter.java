package io.github.wzj.music.ui.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import io.github.ryanhoo.music.ui.base.BaseFragment;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private String[] mTitles;
    private BaseFragment[] mFragments;

    public MainPagerAdapter(FragmentManager fm, String[] titles, BaseFragment[] fragments) {
        super(fm);
        mTitles = titles;
        mFragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments[position];
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public int getCount() {
        if (mTitles == null) return 0;
        return mTitles.length;
    }
}
