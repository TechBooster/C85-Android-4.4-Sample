package com.example.audioeffectsample;

import android.provider.BaseColumns;

public interface Loudness extends BaseColumns {
    public static final String TABLE = "tb_loudness";
    public static final String TRACK_PEAK = "f_peak";
    public static final String TRACK_RMS = "f_rms";
    public static final String TRACK_GAIN = "f_gain";
    public static final String REPLAYGAIN_TRACK_PEAK = "f_replaygain_track_peak";
    public static final String REPLAYGAIN_TRACK_GAIN = "f_replaygain_track_gain";
    public static final String MEDIA_ID = "f_mediaid";

}
