package com.example.audioeffectsample;

import android.provider.BaseColumns;

public interface Loudness extends BaseColumns {
    public static final String TABLE = "tb_loudness";
    public static final String COUNT = "f_count";
    public static final String PEAK_SUM = "f_peak_sum";
    public static final String PEAK_MIN = "f_peak_min";
    public static final String PEAK_MAX = "f_peak_max";
    public static final String RMS_SUM = "f_rms_sum";
    public static final String RMS_MIN = "f_rms_min";
    public static final String RMS_MAX = "f_rms_max";
    public static final String MEDIA_ID = "f_mediaid";

}
