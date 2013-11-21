package com.example.audioeffectsample;

import java.io.IOException;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.MeasurementPeakRms;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements LoaderCallbacks<Cursor>,
        OnItemClickListener, OnClickListener {
    private static final String TAG = "LevelMeterMain";
    private static final int GAIN_MAX = 5000;

    private TextView mTextPeak;
    private TextView mTextLoudnessTitle;
    private TextView mTextRMS;
    private ListView mListView;
    private SeekBar mSeekLoudness;
    private CheckBox mCheckLoudness;
    private EditText mEditTrackGain;

    private LoudnessEnhancer mLoudness;
    private Visualizer mVisualizer;
    private LevelMeterView mLevelMetorPeak;
    private LevelMeterView mLevelMetorRMS;
    private MediaPlayer mMediaPlayer;
    private MediaCursorAdapter mAdapter;

    private Handler mHandler = new Handler();
    private NowPlaying mNowPlaying = new NowPlaying();
    private MySQLiteOpenHelper mSQLiteOpenHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSQLiteOpenHelper = new MySQLiteOpenHelper(this);
        // inizialize
        init();

        // link audiosessionid.
        link(mMediaPlayer.getAudioSessionId());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // disable loudness
        if (mLoudness != null) {
            mLoudness.setEnabled(false);
            mLoudness.release();
            mLoudness = null;
        }
        // disable visualizer
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.release();
        }
        // release mediaplayer
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    public void link(int mAudioSessionId) {

        // create Visualizer and attach audiosessionid
        mVisualizer = new Visualizer(mAudioSessionId);
        mVisualizer.setEnabled(false);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setScalingMode(Visualizer.SCALING_MODE_AS_PLAYED);

        // pass to LevelMetorView capture data
        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer,
                    byte[] bytes, int samplingRate) {
                final MeasurementPeakRms measurement = new MeasurementPeakRms();
                visualizer.getMeasurementPeakRms(measurement);
                mLevelMetorPeak.setPeakRMS(measurement.mPeak);
                mLevelMetorRMS.setPeakRMS(measurement.mRms);

                mNowPlaying.setMeasurementPeakRms(measurement);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder peakText = new StringBuilder();
                        StringBuilder rmsText = new StringBuilder();
                        peakText.append("Peak:")
                                .append(9600 + measurement.mPeak).append("/")
                                .append(mNowPlaying.getMaxPeak());
                        rmsText.append("Rms:").append(9600 + measurement.mRms)
                                .append("/").append(mNowPlaying.getMaxRMS());
                        if (mLoudness.getEnabled()) {
                            float loudness = mLoudness.getTargetGain();
                            peakText.append("/").append(
                                    measurement.mPeak + loudness);
                            rmsText.append("/").append(
                                    measurement.mRms + loudness);
                        }
                        mTextPeak.setText(peakText.toString());
                        mTextRMS.setText(rmsText.toString());
                    }
                });
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
                    int samplingRate) {
            }
        };

        mVisualizer.setDataCaptureListener(captureListener,
                Visualizer.getMaxCaptureRate() / 2, true, false);

        // enable to Visualizer
        mVisualizer.setEnabled(true);
        // enable to mearurement mode
        mVisualizer.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS);
    }

    private void init() {
        mLevelMetorPeak = (LevelMeterView) findViewById(R.id.levelMetorPeak);
        mLevelMetorRMS = (LevelMeterView) findViewById(R.id.levelMetorRMS);

        mTextLoudnessTitle = (TextView) findViewById(R.id.textLoudnessTitle);
        mTextPeak = (TextView) findViewById(R.id.textPeak);
        mTextRMS = (TextView) findViewById(R.id.textRMS);
        mEditTrackGain = (EditText) findViewById(R.id.editTrackGain);
        mListView = (ListView) findViewById(R.id.listView1);
        mListView.setOnItemClickListener(this);
        findViewById(R.id.buttonCalc).setOnClickListener(this);

        // initialize media player
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "onCompletion");
                int currentId = mNowPlaying.getID();
                updateLoudnes(mNowPlaying);
                // play next music
                for (int i = 0; i < mAdapter.getCount(); i++) {
                    Cursor cursor = (Cursor) mAdapter.getItem(i);
                    int id = cursor.getInt(cursor
                            .getColumnIndex(Audio.Media._ID));
                    if (id == currentId) {
                        if (mAdapter.getCount() > (i + 1)) {
                            cursor = (Cursor) mAdapter.getItem(i + 1);
                            id = cursor.getInt(cursor
                                    .getColumnIndex(Audio.Media._ID));
                            startMusic(id);
                        }
                        break;
                    }
                }
                mNowPlaying.reset();
            }
        });

        // attach to Visualizer mediaId
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        // attach to LoudnessEnhancer mediaId
        mLoudness = new LoudnessEnhancer(mMediaPlayer.getAudioSessionId());
        TextView textMin = (TextView) findViewById(R.id.textMin);
        TextView textMax = (TextView) findViewById(R.id.textMax);
        mSeekLoudness = (SeekBar) findViewById(R.id.seekLoudness);
        mCheckLoudness = (CheckBox) findViewById(R.id.checkLoudness);
        mSeekLoudness.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                // set loudness from seekbar position
                int loudness = progress - GAIN_MAX / 2;
                mLoudness.setTargetGain(loudness);
                mTextLoudnessTitle.setText(String.format("%s:%d",
                        getString(R.string.label_loudness), loudness));
            }
        });

        mCheckLoudness.setChecked(mLoudness.getEnabled());
        mCheckLoudness
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        // change to enable state of Loudness
                        mLoudness.setEnabled(isChecked);
                        mNowPlaying.setMeasureing(!mLoudness.getEnabled());
                    }
                });

        int gain = (int) mLoudness.getTargetGain();
        mTextLoudnessTitle.setText(String.format("%s:%d",
                getString(R.string.label_loudness), gain));
        textMin.setText(String.format("%d", -GAIN_MAX / 2));
        textMax.setText(String.format("%d", GAIN_MAX / 2));
        mSeekLoudness.setMax(GAIN_MAX);
        mSeekLoudness.setProgress(gain + GAIN_MAX / 2);

        getLoaderManager().initLoader(0, null, this);
    }

    private void updateLoudnes(NowPlaying now) {
        ContentValues values = new ContentValues();
        values.put(Loudness.MEDIA_ID, now.getID());
        values.put(Loudness.COUNT, now.getCount());
        values.put(Loudness.PEAK_SUM, now.getSumPeak());
        values.put(Loudness.PEAK_MAX, now.getMaxPeak());
        values.put(Loudness.PEAK_MIN, now.getMinPeak());
        values.put(Loudness.RMS_SUM, now.getSumRMS());
        values.put(Loudness.RMS_MAX, now.getMaxRMS());
        values.put(Loudness.RMS_MIN, now.getMinRMS());

        SQLiteDatabase db = mSQLiteOpenHelper.getWritableDatabase();
        if (db != null) {
            Cursor cursor = null;
            try {
                cursor = db.query(Loudness.TABLE,
                        new String[] { Loudness._ID }, Loudness.MEDIA_ID
                                + " = ?",
                        new String[] { Integer.toString(now.getID()) }, null,
                        null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int id = cursor.getInt(0);
                    db.update(Loudness.TABLE, values, Loudness._ID + " = ?",
                            new String[] { Integer.toString(id) });
                } else {
                    db.insert(Loudness.TABLE, null, values);
                }

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
        }
    }

    private void startMusic(int id) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            if (mNowPlaying.getID() == id) {
                return;
            }
        }

        SQLiteDatabase db = mSQLiteOpenHelper.getReadableDatabase();
        if (db != null) {
            Cursor mediaCursor = null;
            try {
                mediaCursor = db
                        .query(Loudness.TABLE, new String[] {
                                Loudness.PEAK_MAX, Loudness.RMS_MAX },
                                Loudness.MEDIA_ID + " = ?",
                                new String[] { Integer.toString(id) }, null,
                                null, null);
                if (mediaCursor != null && mediaCursor.moveToFirst()) {
                    int peakMax = mediaCursor.getInt(0);
                    int rmsMax = mediaCursor.getInt(1);
                    int trackGain = getTrackGain();
                    int gain = Math.abs(peakMax - trackGain);
                    if (trackGain >= 0 && peakMax > 0 && gain > 500) {
                        mCheckLoudness.setChecked(true);
                        if (peakMax < trackGain) {
                            mSeekLoudness.setProgress(GAIN_MAX / 2 + gain);
                        } else {
                            mSeekLoudness.setProgress(GAIN_MAX / 2 - gain);
                        }
                    } else {
                        mCheckLoudness.setChecked(false);
                    }
                } else {
                    mCheckLoudness.setChecked(false);
                }

            } finally {
                if (mediaCursor != null) {
                    mediaCursor.close();
                }
                db.close();
            }
        }

        mNowPlaying.reset();
        mNowPlaying.setMeasureing(!mLoudness.getEnabled());
        mNowPlaying.setID(id);
        mAdapter.notifyDataSetChanged();

        try {
            Uri uri = ContentUris.withAppendedId(
                    Audio.Media.EXTERNAL_CONTENT_URI, id);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(this, uri);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getTrackGain() {
        try {
            String text = mEditTrackGain.getText().toString();
            if (text.length() > 0) {
                return Integer.parseInt(mEditTrackGain.getText().toString());
            }
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(this, Audio.Media.EXTERNAL_CONTENT_URI, null,
                Audio.Media.IS_MUSIC + " = 1", null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        mAdapter = new MediaCursorAdapter(this, cursor, mNowPlaying);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
            long arg3) {
        Cursor cursor = (Cursor) mListView.getItemAtPosition(position);
        int id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));
        startMusic(id);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.buttonCalc){
            SQLiteDatabase db = mSQLiteOpenHelper.getReadableDatabase();
            if (db != null) {
                Cursor cursor = null;
                try {
                    cursor = db
                            .query(Loudness.TABLE, new String[] {
                                    Loudness.PEAK_MAX, Loudness.RMS_MAX }, null, null, null,
                                    null, null);
                    long sumPeak = 0;
                    int count = 0;
                    if (cursor != null && cursor.moveToFirst()) {
                        do{
                            int peakMax = cursor.getInt(0);
                            int rmsMax = cursor.getInt(1);
                            if(peakMax>0){
                                sumPeak += peakMax;
                                count++;
                            }
                        }while(cursor.moveToNext());
                        
                        int avarage = Math.round((float)sumPeak/count);
                        mEditTrackGain.setText(String.format("%d", avarage));
                    }

                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    db.close();
                }
            }
        }
        
    }
}
