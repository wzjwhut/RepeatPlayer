package io.github.ryanhoo.music.event;

import io.github.ryanhoo.music.player.PlayMode;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/5/16
 * Time: 6:32 PM
 * Desc: PlaySongEvent
 */
public class PlayModeEvent {

    public PlayMode mode;

    public PlayModeEvent(PlayMode mode) {
        this.mode = mode;
    }
}
