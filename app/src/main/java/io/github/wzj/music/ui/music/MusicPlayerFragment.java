package io.github.wzj.music.ui.music;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.ryanhoo.music.RxBus;
import io.github.ryanhoo.music.ui.music.MusicPlayerContract;
import io.github.ryanhoo.music.ui.music.MusicPlayerPresenter;
import io.github.wzj.music.R;
import io.github.ryanhoo.music.data.model.SentenceSplitPoint;
import io.github.ryanhoo.music.data.model.PlayList;
import io.github.ryanhoo.music.data.model.Song;
import io.github.ryanhoo.music.data.source.PreferenceManager;
import io.github.ryanhoo.music.data.source.db.LiteOrmHelper;
import io.github.ryanhoo.music.event.PlayListNowEvent;
import io.github.ryanhoo.music.event.PlayModeEvent;
import io.github.ryanhoo.music.event.PlaySongEvent;
import io.github.ryanhoo.music.player.IPlayback;
import io.github.ryanhoo.music.player.PlayMode;
import io.github.ryanhoo.music.player.PlaybackService;
import io.github.ryanhoo.music.ui.base.BaseFragment;
import io.github.ryanhoo.music.utils.TimeUtils;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


public class MusicPlayerFragment extends BaseFragment implements MusicPlayerContract.View, IPlayback.Callback {

    // private static final String TAG = "MusicPlayerFragment";
    private final static Logger logger = LogManager.getLogger("wzj");

    // Update seek bar every second
    private static final long UPDATE_PROGRESS_INTERVAL = 1000;

//    @BindView(R.id.image_view_album)
//    ShadowImageView imageViewAlbum;

    @BindView(R.id.text_view_name)
    TextView textViewName;
    @BindView(R.id.text_view_artist)
    TextView textViewArtist;
    @BindView(R.id.text_view_progress)
    TextView textViewProgress;
    @BindView(R.id.text_view_duration)
    TextView textViewDuration;
    @BindView(R.id.seek_bar)
    SeekBar seekBarProgress;

    @BindView(R.id.repeat_title)
    TextView textViewRepeatTitle;

    @BindView(R.id.button_play_toggle)
    ImageView buttonPlayToggle;

    @BindView(R.id.button_favorite_toggle)
    ImageView buttonFavoriteToggle;

    @BindView(R.id.button_repeat_toggle)
    ImageView buttonRepeatToggle;

    @BindView(R.id.button_repeat_next)
    ImageView buttonRepeatNext;

    @BindView(R.id.button_repeat_last)
    ImageView buttonRepeatLast;

    private IPlayback mPlayer;

    private Handler mHandler = new Handler();

    private MusicPlayerContract.Presenter mPresenter;

    private int mPlayingPosMS = 0;

    private Runnable mProgressCallback = new Runnable() {
        @Override
        public void run() {
            if (isDetached()) return;

            if (mPlayer.isPlaying()) {
                int playPos = mPlayer.getProgress();
                //logger.info("play pos:  {}", playPos);
                mPlayingPosMS = playPos;
                int progress = (int) (seekBarProgress.getMax()
                        * ((float) playPos / (float) getCurrentSongDuration()));
                updateProgressTextWithDuration(mPlayer.getProgress());
                if (progress >= 0 && progress <= seekBarProgress.getMax()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        seekBarProgress.setProgress(progress, true);
                    } else {
                        seekBarProgress.setProgress(progress);
                    }
                    mHandler.postDelayed(this, UPDATE_PROGRESS_INTERVAL);
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateProgressTextWithProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeCallbacks(mProgressCallback);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(seekBar.getProgress() == seekBar.getMax()){
                    logger.error("seek to end");
                    if(mPlayMode == PlayMode.SINGLE){
                        seekTo(0);
                    }else {
                        mPlayer.playNext();
                    }
                }else{
                    seekTo(getDuration(seekBar.getProgress()));
                }

                if (mPlayer.isPlaying()) {
                    mHandler.removeCallbacks(mProgressCallback);
                    mHandler.post(mProgressCallback);
                }
            }
        });
        new MusicPlayerPresenter(getActivity(), this).subscribe();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPlayer != null && mPlayer.isPlaying()) {
            mHandler.removeCallbacks(mProgressCallback);
            mHandler.post(mProgressCallback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mProgressCallback);
    }

    @Override
    public void onDestroyView() {
        mPresenter.unsubscribe();
        super.onDestroyView();
    }

    // Click Events

    @OnClick(R.id.button_play_toggle)
    public void onPlayToggleAction(View view) {

        if (mPlayer == null) return;

        exitRepeatMode();

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.play();
        }
    }

    private void exitRepeatMode(){
        if(mIsRepeating) {
            mIsRepeating = false;
            textViewRepeatTitle.setText("复读");
            buttonRepeatToggle.setImageResource(R.drawable.ic_pause);
            mHandler.removeCallbacks(mRepeatRunnable);
        }
    }

    private boolean mIsRepeating = false;
    private String mPlayingPath;
    private ArrayList<Integer> mPlayPoints;
    private int mRepeatBeginPos;
    private int mRepeatDuration;

    private final Runnable mRepeatRunnable = new Runnable() {
        @Override
        public void run() {
            seekTo(mRepeatBeginPos);
            mHandler.postDelayed(mRepeatRunnable, mRepeatDuration);
        }
    };


    private void entryRepeatMode(int currentPlayPosMS){
        int beginPosMS = 0;
        int endPosMS = 0;
        logger.info("current playMS: {}", currentPlayPosMS);
        for(Integer point : mPlayPoints){
            logger.info("points: {}", point);
            if(point>currentPlayPosMS){
                endPosMS = point;
                break;
            }else{
                beginPosMS = point;
            }
        }
        Song currentSong = mPlayer.getPlayingSong();
        if(endPosMS == 0 ||endPosMS >= currentSong.getDuration()){
            endPosMS = currentSong.getDuration() - 500;
        }

        int duration = endPosMS - beginPosMS;
        if(duration<=0){
            Toast.makeText(getContext(), "复读时间太短", Toast.LENGTH_SHORT).show();
            textViewRepeatTitle.setText("复读");
            buttonRepeatToggle.setImageResource(R.drawable.ic_play);
            return;
        }

        logger.info("repeat range: {}, {}", beginPosMS, endPosMS);
        mRepeatBeginPos = beginPosMS;
        mRepeatDuration = duration;

        textViewRepeatTitle.setText("正在复读: "  + TimeUtils.formatDuration(mRepeatBeginPos) + " - " + TimeUtils.formatDuration(mRepeatBeginPos+mRepeatDuration));
        buttonRepeatToggle.setImageResource(R.drawable.ic_pause);

        seekTo(beginPosMS);
        mPlayer.play();
        mHandler.postDelayed(mRepeatRunnable, duration);
    }

    private void entryRepeatModeLast(int currentPlayPosMS){
        int beginPosMS = 0;
        int endPosMS = 0;
        logger.info("current playMS: {}", currentPlayPosMS);
        int i = 0;
        for(Integer point : mPlayPoints){
            logger.info("points: {}, i: {}", point, i);
            if(point>currentPlayPosMS){
                break;
            }
            i++;
        }
        if(i == 0){
            exitRepeatMode();
            return;
        }

        endPosMS = mPlayPoints.get(i-1);
        if(i-2>=0){
            beginPosMS = mPlayPoints.get(i-2);
        }

        Song currentSong = mPlayer.getPlayingSong();
        if(endPosMS == 0 ||endPosMS >= currentSong.getDuration()){
            endPosMS = currentSong.getDuration() - 500;
        }

        int duration = endPosMS - beginPosMS;
        if(duration<=0){
            Toast.makeText(getContext(), "复读时间太短", Toast.LENGTH_SHORT).show();
            exitRepeatMode();
            return;
        }

        logger.info("repeat range: {}, {}", beginPosMS, endPosMS);
        mRepeatBeginPos = beginPosMS;
        mRepeatDuration = duration;
        mIsRepeating = true;
        textViewRepeatTitle.setText("正在复读: "  + TimeUtils.formatDuration(mRepeatBeginPos) + " - " + TimeUtils.formatDuration(mRepeatBeginPos+mRepeatDuration));
        buttonRepeatToggle.setImageResource(R.drawable.ic_pause);

        seekTo(beginPosMS);
        mPlayer.play();
        mHandler.postDelayed(mRepeatRunnable, duration);
    }

    private void entryRepeatModeNext(int currentPlayPosMS){
        int beginPosMS = 0;
        int endPosMS = 0;
        logger.info("current playMS: {}", currentPlayPosMS);
        int i = 0;
        for(Integer point : mPlayPoints){
            logger.info("points: {}", point);
            if(point>currentPlayPosMS){
                beginPosMS = point;
                break;
            }
            i++;
        }
        if(i>=mPlayPoints.size()){
            Toast.makeText(getContext(), "复读时间太短", Toast.LENGTH_SHORT).show();
            exitRepeatMode();
            return;
        }
        endPosMS = mPlayPoints.get((i+1));

        Song currentSong = mPlayer.getPlayingSong();
        if(endPosMS == 0 ||endPosMS >= currentSong.getDuration()){
            endPosMS = currentSong.getDuration() - 500;
        }

        int duration = endPosMS - beginPosMS;
        if(duration<=0){
            Toast.makeText(getContext(), "复读时间太短", Toast.LENGTH_SHORT).show();
            exitRepeatMode();
            return;
        }

        logger.info("repeat range: {}, {}", beginPosMS, endPosMS);
        mRepeatBeginPos = beginPosMS;
        mRepeatDuration = duration;
        mIsRepeating = true;
        textViewRepeatTitle.setText("正在复读: "  + TimeUtils.formatDuration(mRepeatBeginPos) + " - " + TimeUtils.formatDuration(mRepeatBeginPos+mRepeatDuration));
        buttonRepeatToggle.setImageResource(R.drawable.ic_pause);

        seekTo(beginPosMS);
        mPlayer.play();
        mHandler.postDelayed(mRepeatRunnable, duration);
    }


    ProgressDialog progressDialog;
    public void showProgressDialog(Context mContext, String text) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.setMessage(text);    //设置内容
        progressDialog.setCancelable(false);//点击屏幕和按返回键都不能取消加载框
        progressDialog.show();
    }

    public Boolean dismissProgressDialog() {
        if (progressDialog != null){
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                return true;//取消成功
            }
        }
        return false;//已经取消过了，不需要取消
    }

    @OnClick(R.id.button_repeat_next)
    public void onRepeatNext(View view){
        if(mPlayer.getPlayingSong() == null){
            return;
        }
        exitRepeatMode();
        mPlayer.pause();
        if(mPlayPoints == null || mPlayPoints.isEmpty()){
            onRepeatToggleAction(view);
            return;
        }
        final int playPos;
        if(mPlayer.isPlaying()){
            playPos = mPlayer.getProgress();
        }else{
            playPos = mPlayingPosMS;
        }
        //进入复读模式.

        entryRepeatModeNext(playPos);
    }

    @OnClick(R.id.button_repeat_last)
    public void onRepeatLast(View view){
        if(mPlayer.getPlayingSong() == null){
            return;
        }
        exitRepeatMode();
        mPlayer.pause();
        if(mPlayPoints == null || mPlayPoints.isEmpty()){
            onRepeatToggleAction(view);
            return;
        }
        final int playPos;
        if(mPlayer.isPlaying()){
            playPos = mPlayer.getProgress();
        }else{
            playPos = mPlayingPosMS;
        }
        //进入复读模式.
        entryRepeatModeLast(playPos);

    }

    @OnClick(R.id.button_repeat_toggle)
    public void onRepeatToggleAction(View view) {
        logger.info("click repeat");
        if(mPlayer.getPlayingSong() == null){
            return;
        }

        if(mIsRepeating){
           //退出复读模式
            logger.info("exit repeat mode");
            textViewRepeatTitle.setText("复读");
            buttonRepeatToggle.setImageResource(R.drawable.ic_play);
            mIsRepeating = false;
            mHandler.removeCallbacks(mRepeatRunnable);
        }else{
            logger.info("try to entry repeat mode");
            buttonRepeatToggle.setImageResource(R.drawable.ic_pause);
            mIsRepeating = true;

            final int playPos;
            if(mPlayer.isPlaying()){
                playPos = mPlayer.getProgress();
            }else{
                playPos = mPlayingPosMS;
            }
            //进入复读模式.
            mPlayer.pause();

            final String playingPath = mPlayer.getPlayingSong().getPath();
            if(mPlayPoints != null && playingPath.equals(mPlayingPath) && mPlayPoints != null){
                logger.info("points exist, directly entry repeat mode");
                entryRepeatMode(playPos);
                return;
            }

            SentenceSplitPoint point = LiteOrmHelper.getInstance().queryById(playingPath, SentenceSplitPoint.class);
            if(point != null){
                logger.info("get break points success {}", playingPath);
                mPlayPoints = point.getBreakPoints();
                mPlayingPath = playingPath;
                entryRepeatMode(playPos);
            }else{
                //Toast.makeText(getContext(), "正在分析, 请稍候", Toast.LENGTH_LONG).show();
                showProgressDialog(getContext(), "正在分析, 请稍候");
                final SplitMP3Sentence praser = new SplitMP3Sentence(playingPath);
                praser.setOnCompleteListener(new SplitMP3Sentence.OnCompleteListener() {
                    @Override
                    public void completed() {
                        ArrayList<Integer> points =  praser.getPointsList();
                        if(points == null || points.isEmpty()){
                            Toast.makeText(getContext(), "分析失败", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getContext(), "分析成功", Toast.LENGTH_SHORT).show();
                            SentenceSplitPoint point = new SentenceSplitPoint();
                            point.setPath(playingPath);
                            point.setPoints(StringUtils.join(points, ','));
                            LiteOrmHelper.getInstance().save(point);
                            mPlayPoints = point.getBreakPoints();
                            mPlayingPath = playingPath;
                            logger.info("get points success, points count: {}", points.size());
                            entryRepeatMode(playPos);
                        }
                        dismissProgressDialog();
                    }
                });
                praser.start();
            }
        }
    }

//    @OnClick(R.id.button_play_mode_toggle)
//    public void onPlayModeToggleAction(View view) {
//        if (mPlayer == null) return;
//
//        PlayMode current = PreferenceManager.lastPlayMode(getActivity());
//        PlayMode newMode = PlayMode.switchNextMode(current);
//        PreferenceManager.setPlayMode(getActivity(), newMode);
//        mPlayer.setPlayMode(newMode);
//        updatePlayMode(newMode);
//    }

    @OnClick(R.id.button_play_last)
    public void onPlayLastAction(View view) {
        if (mPlayer == null) return;

        mPlayer.playLast();
    }

    @OnClick(R.id.button_play_next)
    public void onPlayNextAction(View view) {
        if (mPlayer == null) return;

        mPlayer.playNext();
    }

    @OnClick(R.id.button_favorite_toggle)
    public void onFavoriteToggleAction(View view) {
        if (mPlayer == null) return;

        Song currentSong = mPlayer.getPlayingSong();
        if (currentSong != null) {
            view.setEnabled(false);
            mPresenter.setSongAsFavorite(currentSong, !currentSong.isFavorite());
        }
    }

    @OnClick(R.id.button_3_sec)
    public void on3Sec(View view) {
        repeatPlaySec(3);
    }

    @OnClick(R.id.button_5_sec)
    public void on5Sec(View view) {
        repeatPlaySec(5);
    }

    @OnClick(R.id.button_8_sec)
    public void on8Sec(View view) {
        repeatPlaySec(8);
    }

    @OnClick(R.id.button_10_sec)
    public void on10Sec(View view) {
        repeatPlaySec(10);
    }

    private void repeatPlaySec(int sec){
        if(mPlayer.getPlayingSong() == null){
            return;
        }
        if(mIsRepeating){
            return;
        }
        logger.info("try to entry repeat mode");

        final int currentPlayPosMS;
        if(mPlayer.isPlaying()){
            currentPlayPosMS = mPlayer.getProgress();
        }else{
            currentPlayPosMS = mPlayingPosMS;
        }
        //进入复读模式.
        mPlayer.pause();

        int beginPosMS = currentPlayPosMS - sec*1000;
        if(beginPosMS < 0){
            beginPosMS = 0;
        }
        int endPosMS = currentPlayPosMS;
        int duration = endPosMS - beginPosMS;
        if(duration<=0){
            Toast.makeText(getContext(), "复读时间太短", Toast.LENGTH_SHORT).show();
            return;
        }
        textViewRepeatTitle.setText("正在复读: "  + TimeUtils.formatDuration(beginPosMS) + " - " + TimeUtils.formatDuration(endPosMS));
        buttonRepeatToggle.setImageResource(R.drawable.ic_pause);
        mIsRepeating = true;

        logger.info("repeat range: {}, {}", beginPosMS, endPosMS);
        mRepeatBeginPos = beginPosMS;
        mRepeatDuration = duration;
        seekTo(beginPosMS);
        mPlayer.play();
        mHandler.postDelayed(mRepeatRunnable, duration);
    }

    // RXBus Events

    PlayMode mPlayMode = PlayMode.getDefault();

    @Override
    protected Subscription subscribeEvents() {
        return RxBus.getInstance().toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        if (o instanceof PlaySongEvent) {
                            logger.error("on playsong event");
                            onPlaySongEvent((PlaySongEvent) o);
                        } else if (o instanceof PlayListNowEvent) {
                            logger.error("[event] PlayListNowEvent");
                            onPlayListNowEvent((PlayListNowEvent) o);
                        } else if (o instanceof PlayModeEvent) {
                            PlayMode newMode = ((PlayModeEvent) o).mode;
                            mPlayMode = newMode;
                            logger.info("on new play mode: {}", newMode);
                            if(mPlayer != null) {
                                mPlayer.setPlayMode(newMode);
                            }
                        }
                    }
                })
                .subscribe(RxBus.defaultSubscriber());
    }

    private void onPlaySongEvent(PlaySongEvent event) {
        Song song = event.song;
        playSong(song);
    }

    private void onPlayListNowEvent(PlayListNowEvent event) {
        PlayList playList = event.playList;
        int playIndex = event.playIndex;
        playSong(playList, playIndex);
        exitRepeatMode();
    }

    // Music Controls

    private void playSong(Song song) {
        PlayList playList = new PlayList(song);
        playSong(playList, 0);
    }

    private void playSong(PlayList playList, int playIndex) {
        if (playList == null) return;

        playList.setPlayMode(PreferenceManager.lastPlayMode(getActivity()));
        // boolean result =
        //mPlayer.setPlayList(playList);
        mPlayer.play(playList);
        //mPlayer.play(playList, playIndex);

        Song song = playList.getCurrentSong();
        onSongUpdated(song);

        /*
        seekBarProgress.setProgress(0);
        seekBarProgress.setEnabled(result);
        textViewProgress.setText(R.string.mp_music_default_duration);

        if (result) {
            imageViewAlbum.startRotateAnimation();
            buttonPlayToggle.setImageResource(R.drawable.ic_pause);
            textViewDuration.setText(TimeUtils.formatDuration(song.getDuration()));
        } else {
            buttonPlayToggle.setImageResource(R.drawable.ic_play);
            textViewDuration.setText(R.string.mp_music_default_duration);
        }

        mHandler.removeCallbacks(mProgressCallback);
        mHandler.post(mProgressCallback);

        getActivity().startService(new Intent(getActivity(), PlaybackService.class));
        */
    }

    private void updateProgressTextWithProgress(int progress) {
        int targetDuration = getDuration(progress);
        textViewProgress.setText(TimeUtils.formatDuration(targetDuration));
    }

    private void updateProgressTextWithDuration(int duration) {
        textViewProgress.setText(TimeUtils.formatDuration(duration));
    }

    private void seekTo(int duration) {
        logger.info("seek to: {}", duration);
        mPlayingPosMS = duration;
        if(!mPlayer.isPlaying()){
            mPlayer.play();
        }
        mPlayer.seekTo(duration);
    }

    private int getDuration(int progress) {
        return (int) (getCurrentSongDuration() * ((float) progress / seekBarProgress.getMax()));
    }

    private int getCurrentSongDuration() {
        Song currentSong = mPlayer.getPlayingSong();
        int duration = 0;
        if (currentSong != null) {
            duration = currentSong.getDuration();
        }
        return duration;
    }

    // Player Callbacks

    @Override
    public void onSwitchLast(Song last) {
        logger.info("onSwitchLast");
        onSongUpdated(last);
    }

    @Override
    public void onSwitchNext(Song next) {
        logger.info("onSwitchNext");
        onSongUpdated(next);
    }

    @Override
    public void onComplete(Song next) {
        logger.info("[MusicFragment] onSong complete " + next);
        onSongUpdated(next);
    }

    @Override
    public void onPlayStatusChanged(boolean isPlaying) {
        updatePlayToggle(isPlaying);
        if (isPlaying) {
            mHandler.removeCallbacks(mProgressCallback);
            mHandler.post(mProgressCallback);
        } else {
            mHandler.removeCallbacks(mProgressCallback);
        }
    }

    // MVP View

    @Override
    public void handleError(Throwable error) {
        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPlaybackServiceBound(PlaybackService service) {
        mPlayer = service;
        mPlayer.registerCallback(this);
        mPlayMode = PreferenceManager.lastPlayMode(getContext());
        PlayList playList = PreferenceManager.getPlayList(this.getContext());
        if(playList != null){
            playList.setPlayMode(mPlayMode);
            mPlayer.setPlayList(playList);
        }
        //Player player = Player.getInstance();
        //PlayList list = player.getmPlayList();
        //Log.e("wzj", "play list " + list.getNumOfSongs());
    }

    @Override
    public void onPlaybackServiceUnbound() {
        mPlayer.unregisterCallback(this);
        mPlayer = null;
    }

    @Override
    public void onSongSetAsFavorite(@NonNull Song song) {
        buttonFavoriteToggle.setEnabled(true);
        updateFavoriteToggle(song.isFavorite());
    }

    public void onSongUpdated(@Nullable Song song) {
        exitRepeatMode();
        this.mPlayPoints = null;
        if (song == null) {
            //imageViewAlbum.cancelRotateAnimation();
            buttonPlayToggle.setImageResource(R.drawable.ic_play);
            seekBarProgress.setProgress(0);
            updateProgressTextWithProgress(0);
            seekTo(0);
            mHandler.removeCallbacks(mProgressCallback);
            return;
        }

        // Step 1: Song name and artist
        textViewName.setText(song.getDisplayName());
        textViewArtist.setText(song.getArtist());
        // Step 2: favorite
        buttonFavoriteToggle.setImageResource(song.isFavorite() ? R.drawable.ic_favorite_yes : R.drawable.ic_favorite_no);
        // Step 3: Duration
        textViewDuration.setText(TimeUtils.formatDuration(song.getDuration()));
        // Step 4: Keep these things updated
        // - Album rotation
        // - Progress(textViewProgress & seekBarProgress)
//        Bitmap bitmap = AlbumUtils.parseAlbum(song);
//        if (bitmap == null) {
//            imageViewAlbum.setImageResource(R.drawable.default_record_album);
//        } else {
//            imageViewAlbum.setImageBitmap(AlbumUtils.getCroppedBitmap(bitmap));
//        }
//        imageViewAlbum.pauseRotateAnimation();
        mHandler.removeCallbacks(mProgressCallback);
        if (mPlayer.isPlaying()) {
//            imageViewAlbum.startRotateAnimation();
            mHandler.post(mProgressCallback);
            buttonPlayToggle.setImageResource(R.drawable.ic_pause);
        }
    }

    @Override
    public void updatePlayMode(PlayMode playMode) {
        if (playMode == null) {
            playMode = PlayMode.getDefault();
        }
    }

    @Override
    public void updatePlayToggle(boolean play) {
        buttonPlayToggle.setImageResource(play ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    public void updateFavoriteToggle(boolean favorite) {
        buttonFavoriteToggle.setImageResource(favorite ? R.drawable.ic_favorite_yes : R.drawable.ic_favorite_no);
    }

    @Override
    public void setPresenter(MusicPlayerContract.Presenter presenter) {
        mPresenter = presenter;
    }
}
