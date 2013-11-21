package com.example.audioeffectsample;

import android.media.audiofx.Visualizer.MeasurementPeakRms;

public class NowPlaying {
    private int mID;
    private long mSumPeak;
    private long mSumRMS;
    private long mCount;
    private int mMinPeak;
    private int mMinRMS;
    private int mMaxPeak;
    private int mMaxRMS;
    private boolean mMeasureing;

    public NowPlaying() {
        reset();
    }

    public int getID() {
        return mID;
    }

    public void setID(int iD) {
        mID = iD;
    }

    public long getSumPeak() {
        return mSumPeak;
    }

    public void setSumPeak(long sumPeak) {
        mSumPeak = sumPeak;
    }

    public long getSumRMS() {
        return mSumRMS;
    }

    public void setSumRMS(long sumRMS) {
        mSumRMS = sumRMS;
    }

    public long getCount() {
        return mCount;
    }

    public void setCount(long count) {
        mCount = count;
    }

    public int getMinPeak() {
        return mMinPeak;
    }

    public void setMinPeak(int minPeak) {
        mMinPeak = minPeak;
    }

    public int getMinRMS() {
        return mMinRMS;
    }

    public void setMinRMS(int minRMS) {
        mMinRMS = minRMS;
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

    public boolean isMeasureing() {
        return mMeasureing;
    }

    public void setMeasureing(boolean measureing) {
        mMeasureing = measureing;
    }

    public void reset() {
        mSumPeak = 0;
        mSumRMS = 0;
        mCount = 0;
        mMinPeak = 9600;
        mMinRMS = 9600;
        mMaxPeak = 0;
        mMaxRMS = 0;
        mMeasureing = true;
    }

    public void setMeasurementPeakRms(MeasurementPeakRms measurement) {
        mCount++;
        int peak = 9600 + measurement.mPeak;
        int rms = 9600 + measurement.mRms;
        mSumPeak += peak;
        mSumRMS += rms;

        if (mMaxPeak < peak) {
            mMaxPeak = peak;
        }
        if (mMaxRMS < rms) {
            mMaxRMS = rms;
        }
        if (mMinPeak > peak) {
            mMinPeak = peak;
        }
        if (mMinRMS > rms) {
            mMinRMS = rms;
        }
    }
}
