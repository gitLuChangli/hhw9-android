package com.example.handheld.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.example.handheld.R;

public class Beep {

    private SoundPool soundPool;
    private int sound_ok;
    private int sound_error;
    private int sound_inv;

    private int sound_current;

    public Beep(Context context) {
        this.soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
        this.sound_ok = soundPool.load(context, R.raw.success, 0);
        this.sound_error = soundPool.load(context, R.raw.error, 0);
        this.sound_inv = soundPool.load(context, R.raw.ding, 0);
    }

    public void playOk() {
        if (sound_current > 0) {
            soundPool.stop(sound_current);
        }
        sound_current = soundPool.play(sound_ok, 1f, 1f, 0, 0, 1f);
    }

    public void playError() {
        if (sound_current > 0) {
            soundPool.stop(sound_current);
        }
        sound_current = soundPool.play(sound_error, 1f, 1f, 0, 0, 1f);
    }

    public void playInv() {
        if (sound_current > 0) {
            soundPool.stop(sound_current);
        }
        sound_current = soundPool.play(sound_inv, 1f, 1f, 0, 0, 1f);
    }
}
