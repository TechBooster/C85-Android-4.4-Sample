package com.example.audioeffectsample;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;

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
import android.os.Handler.Callback;
import android.os.Message;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
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
        OnItemClickListener, OnClickListener, Callback {
    private static final String TAG = "LevelMeterMain";
    private static final int MSG_ = 0;
    private static final int GAIN_MAX = 8000;
    private static final int GAIN_LIMIT = 60;

    private TextView mTextVolume;
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

    private Handler mHandler = new Handler(this);
    private NowPlaying mNowPlaying = new NowPlaying();
    private MySQLiteOpenHelper mSQLiteOpenHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_main);

        mSQLiteOpenHelper = new MySQLiteOpenHelper(this);
        // inizialize
        init();

        // link audiosessionid.
        link(mMediaPlayer.getAudioSessionId());
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
            mVisualizer.setMeasurementMode(Visualizer.MEASUREMENT_MODE_NONE);
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
                if (visualizer.getEnabled()) {
                    MeasurementPeakRms measurement = new MeasurementPeakRms();
                    visualizer.getMeasurementPeakRms(measurement);
                    mLevelMetorPeak.setPeakRMS(measurement.mPeak);
                    mLevelMetorRMS.setPeakRMS(measurement.mRms);

                    mNowPlaying.setMeasurementPeakRms(measurement);

                    mHandler.sendMessage(mHandler.obtainMessage(MSG_,
                            measurement));
                }
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
        mTextVolume = (TextView) findViewById(R.id.textVolume);
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
                if (currentId == -1) {
                    mEditTrackGain.setText(Integer.toString(mNowPlaying
                            .getMaxPeak()));
                } else {
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
                                String data = cursor.getString(cursor
                                        .getColumnIndex(Audio.Media.DATA));
                                // update replay gain info
                                updateReplayGain(id, data);
                                // start next music
                                startMusic(id);
                            }
                            break;
                        }
                    }
                }
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
                        mNowPlaying.setLoudnessEnable(mLoudness.getEnabled());
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
        if (!now.isLoudnessEnable()) {
            ContentValues values = new ContentValues();
            values.put(Loudness.MEDIA_ID, now.getID());
            values.put(Loudness.TRACK_PEAK, now.getMaxPeak());
            values.put(Loudness.TRACK_RMS, now.getMaxRMS());

            SQLiteDatabase db = mSQLiteOpenHelper.getWritableDatabase();
            if (db != null) {
                Cursor cursor = null;
                try {
                    cursor = db.query(Loudness.TABLE,
                            new String[] { Loudness._ID }, Loudness.MEDIA_ID
                                    + " = ?",
                            new String[] { Integer.toString(now.getID()) },
                            null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int id = cursor.getInt(0);
                        db.update(Loudness.TABLE, values,
                                Loudness._ID + " = ?",
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
        String data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));

        // update replay gain info
        updateReplayGain(id, data);
        // start music
        startMusic(id);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonCalc) {
            startCalculation();
        }
    }

    private void startMusic(int id) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            if (mNowPlaying.getID() == id) {
                return;
            }
        }

        // reset measure peak
        mNowPlaying.reset();
        mNowPlaying.setLoudnessEnable(mLoudness.getEnabled());

        // check track gain
        int gain = 0;
        SQLiteDatabase db = mSQLiteOpenHelper.getReadableDatabase();
        if (db != null) {
            Cursor mediaCursor = null;
            try {
                mediaCursor = db
                        .query(Loudness.TABLE,
                                new String[] { Loudness.TRACK_GAIN },
                                Loudness.MEDIA_ID + " = ?",
                                new String[] { Integer.toString(id) }, null,
                                null, null);
                if (mediaCursor != null && mediaCursor.moveToFirst()) {
                    int track_gain = mediaCursor.getInt(0);
                    if (Math.abs(track_gain) > GAIN_LIMIT) {
                        gain = track_gain;
                    }
                }
            } finally {
                if (mediaCursor != null) {
                    mediaCursor.close();
                }
                db.close();
            }
        }

        // set loudness gain
        mSeekLoudness.setProgress(GAIN_MAX / 2 + gain);

        mNowPlaying.reset();
        mNowPlaying.setLoudnessEnable(mLoudness.getEnabled());
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

    private void startCalculation() {
        //select median
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT AVG(").append(Loudness.TRACK_PEAK).append(")");
        sql.append(" FROM (SELECT ").append(Loudness.TRACK_PEAK);
        sql.append(" FROM ").append(Loudness.TABLE);
        sql.append(" ORDER BY ").append(Loudness.TRACK_PEAK);
        sql.append(" LIMIT 2 - (SELECT COUNT(*) FROM ").append(Loudness.TABLE)
                .append(") % 2");
        sql.append(" OFFSET (SELECT (COUNT(*) - 1) / 2");
        sql.append(" FROM ").append(Loudness.TABLE).append("))");

        SQLiteDatabase db = mSQLiteOpenHelper.getWritableDatabase();
        if (db != null) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(sql.toString(), null);
                if (cursor != null && cursor.moveToFirst()) {
                    int reference_peak = cursor.getInt(0);

                    // set reference peak;
                    mEditTrackGain.setText(Integer.toString(reference_peak));
                    cursor.close();
                    cursor = db.query(Loudness.TABLE, new String[] {
                            Loudness._ID, Loudness.TRACK_PEAK }, null, null,
                            null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        // set gain
                        do {
                            int id = cursor.getInt(0);
                            int peak = cursor.getInt(1);
                            int gain = reference_peak - peak;
                            ContentValues values = new ContentValues();
                            values.put(Loudness.TRACK_GAIN, gain);
                            db.update(Loudness.TABLE, values, Loudness._ID
                                    + " = ?",
                                    new String[] { Integer.toString(id) });
                        } while (cursor.moveToNext());
                        mAdapter.notifyDataSetChanged();
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
        }
    }

    private void updateReplayGain(int mediaid, String data) {
        ContentValues values = new ContentValues();
        MP3File mp3File;
        try {
            mp3File = new MP3File(new File(data));
            ID3v24Tag v24Tag = mp3File.getID3v2TagAsv24();

            Iterator i = v24Tag.getFrameOfType("TXXX");
            while (i.hasNext()) {
                Object obj = i.next();
                if (obj instanceof AbstractID3v2Frame) {
                    AbstractTagFrameBody af = ((AbstractID3v2Frame) obj)
                            .getBody();
                    if (af instanceof FrameBodyTXXX) {
                        FrameBodyTXXX fb = (FrameBodyTXXX) af;
                        if (fb.getDescription().equals("replaygain_track_peak")) {
                            values.put(Loudness.REPLAYGAIN_TRACK_PEAK, Utils
                                    .parseFloat(fb
                                            .getTextWithoutTrailingNulls()));
                        } else if (fb.getDescription().equals(
                                "replaygain_track_gain")) {
                            values.put(Loudness.REPLAYGAIN_TRACK_GAIN, Utils
                                    .parseFloat(fb
                                            .getTextWithoutTrailingNulls()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TagException e) {
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
        }

        if (values.size() > 0) {
            SQLiteDatabase db = mSQLiteOpenHelper.getWritableDatabase();
            if (db != null) {
                Cursor cursor = null;
                try {
                    cursor = db.query(Loudness.TABLE,
                            new String[] { Loudness._ID }, Loudness.MEDIA_ID
                                    + " = ?",
                            new String[] { Integer.toString(mediaid) }, null,
                            null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int id = cursor.getInt(0);
                        db.update(Loudness.TABLE, values,
                                Loudness._ID + " = ?",
                                new String[] { Integer.toString(id) });
                    } else {
                        values.put(Loudness.MEDIA_ID, mediaid);
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

    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_) {
            MeasurementPeakRms measurement = (MeasurementPeakRms) msg.obj;
            StringBuilder peakText = new StringBuilder();
            StringBuilder rmsText = new StringBuilder();
            peakText.append("Peak:").append(measurement.mPeak).append("/")
                    .append(mNowPlaying.getMaxPeak());
            rmsText.append("Rms:").append(measurement.mRms).append("/")
                    .append(mNowPlaying.getMaxRMS());
            mTextPeak.setText(peakText.toString());
            mTextRMS.setText(rmsText.toString());
        }
        return false;
    }
}
