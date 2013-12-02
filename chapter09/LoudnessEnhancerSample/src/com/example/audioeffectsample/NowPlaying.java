package com.example.audioeffectsample;

import android.media.audiofx.Visualizer.MeasurementPeakRms;

public class NowPlaying {
    private int mID;
    private int mMaxPeak;
    private int mMaxRMS;
    private boolean mLoudnessEnable;

    public NowPlaying() {
        reset();
    }

    public int getID() {
        return mID;
    }

    public void setID(int iD) {
        mID = iD;
    }

    public int getMaxPeak() {
        return mMaxPeak;
    }

    public void setMaxPeak(int maxPeak) {
        mMaxPeak = maxPeak;
    }

    public int getMaxRMS() {
        return mMaxRMS;
    }

    public void setMaxRMS(int maxRMS) {
        mMaxRMS = maxRMS;
    }

    public boolean isLoudnessEnable() {
        return mLoudnessEnable;
    }

    public void setLoudnessEnable(boolean loudness) {
        if(!mLoudnessEnable){
            mLoudnessEnable = loudness;
        }
    }

    public void reset() {
        mMaxPeak = -9600;
        mMaxRMS = -9600;
        mLoudnessEnable = false;
    }

    public void setMeasurementPeakRms(MeasurementPeakRms measurement) {
        int peak = measurement.mPeak;
        int rms = measurement.mRms;

        if (mMaxPeak < peak) {
            mMaxPeak = peak;
        }
        if (mMaxRMS < rms) {
            mMaxRMS = rms;
        }
    }
}
