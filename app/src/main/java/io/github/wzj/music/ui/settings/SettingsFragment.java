package io.github.wzj.music.ui.settings;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.wzj.music.R;
import io.github.ryanhoo.music.RxBus;
import io.github.ryanhoo.music.data.source.PreferenceManager;
import io.github.ryanhoo.music.event.PlayListNowEvent;
import io.github.ryanhoo.music.event.PlayModeEvent;
import io.github.ryanhoo.music.event.PlaySongEvent;
import io.github.ryanhoo.music.player.PlayMode;
import io.github.ryanhoo.music.ui.base.BaseFragment;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class SettingsFragment extends BaseFragment {

    @BindView(R.id.button_play_mode_toggle)
    ImageView buttonPlayModeToggle;

    @BindView(R.id.text_play_mode)
    TextView textPlayMode;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        PlayMode playMode = PreferenceManager.lastPlayMode(getActivity());
        Log.e("wzj", "last play mode: " + playMode);
        initPlayModeView(playMode);
        RxBus.getInstance().post(new PlayModeEvent(playMode));
    }


    @Override
    protected Subscription subscribeEvents() {
        return RxBus.getInstance().toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        if (o instanceof PlaySongEvent) {

                        } else if (o instanceof PlayListNowEvent) {

                        }
                    }
                })
                .subscribe(RxBus.defaultSubscriber());
    }

    private void initPlayModeView(PlayMode mode){
        //PlayMode current = PreferenceManager.lastPlayMode(getActivity());
        if(mode == null){
            mode = PlayMode.getDefault();
        }

        switch (mode) {
            case LIST:  //顺序播放
                buttonPlayModeToggle.setImageResource(R.drawable.ic_play_mode_list);
                textPlayMode.setText("顺序播放");
                break;
            case LOOP:  //列表循环
                buttonPlayModeToggle.setImageResource(R.drawable.ic_play_mode_loop);
                textPlayMode.setText("列表循环");
                break;
            case SHUFFLE: //随机播放
                buttonPlayModeToggle.setImageResource(R.drawable.ic_play_mode_shuffle);
                textPlayMode.setText("随机播放");
                break;
            case SINGLE: //单曲循环
                buttonPlayModeToggle.setImageResource(R.drawable.ic_play_mode_single);
                textPlayMode.setText("单曲循环");
                break;
        }
    }

    @OnClick(R.id.switch_playmode)
    public void onPlayModeToggleAction(View view) {
        PlayMode current = PreferenceManager.lastPlayMode(getActivity());
        PlayMode newMode = PlayMode.switchNextMode(current);
        PreferenceManager.setPlayMode(getActivity(), newMode);
        initPlayModeView(newMode);
        RxBus.getInstance().post(new PlayModeEvent(newMode));
    }

}
