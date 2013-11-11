package com.example.audioeffectsample;

import android.app.Activity;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.MeasurementPeakRms;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "LevelMeterMain";

    private static final int GAIN_MAX = 5000;

    private TextView mTextPeak;
    private TextView mTextRMS;
    private TextView mTextPos;

    private int mAudioSessionId = 0;
    private LoudnessEnhancer mLoudness;
    private Visualizer mVisualizer;
    private LevelMeterView mLevelMetorPeak;
    private LevelMeterView mLevelMetorRMS;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初期化
        init();

        link(mAudioSessionId);
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
        //本アプリ終了時にLoudnessの効果を無効にする
        if (mLoudness != null) {
            mLoudness.setEnabled(false);
            mLoudness.release();
            mLoudness = null;
        }
        if (mVisualizer != null) {
            mVisualizer.release();
        }
    }

    public void link(int mAudioSessionId) {
        // Visualizerオブジェクトを生成し再生中のMediaPlayerに紐付ける
        mVisualizer = new Visualizer(mAudioSessionId);
        mVisualizer.setEnabled(false);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setScalingMode(Visualizer.SCALING_MODE_AS_PLAYED);

        // Visualizerに入るデータをLevelMetorViewに渡す
        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer,
                    byte[] bytes, int samplingRate) {
                final MeasurementPeakRms measurement = new MeasurementPeakRms();
                visualizer.getMeasurementPeakRms(measurement);
                mLevelMetorPeak.setPeakRMS(measurement.mPeak);
                mLevelMetorRMS.setPeakRMS(measurement.mRms);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextPeak.setText("Peak:" + measurement.mPeak);
                        mTextRMS.setText("Rms:" + measurement.mRms);
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
        
        // Visualizerを有効にする
        mVisualizer.setEnabled(true);
        // Peak,RMSの計測を有効にする
        mVisualizer.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS);
    }

    private void init() {
        mLevelMetorPeak = (LevelMeterView) findViewById(R.id.levelMetorPeak);
        mLevelMetorRMS = (LevelMeterView) findViewById(R.id.levelMetorRMS);

        mTextPeak = (TextView) findViewById(R.id.textPeak);
        mTextRMS = (TextView) findViewById(R.id.textRMS);
        mTextPos = (TextView) findViewById(R.id.textPos);

        //Visualizerを再生中のオーディオにアタッチする
        mVisualizer = new Visualizer(mAudioSessionId);
        //LoudnessEnhancerを再生中のオーディオにアタッチする
        mLoudness = new LoudnessEnhancer(mAudioSessionId);
        TextView textMax = (TextView) findViewById(R.id.textMax);
        SeekBar seekbar = (SeekBar) findViewById(R.id.seekBar1);
        CheckBox checkbox1 = (CheckBox) findViewById(R.id.checkBox1);
        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                //SeekBarの値をLoudnessに設定する
                mLoudness.setTargetGain(progress);
                mTextPos.setText(Integer.toString(progress));
            }
        });

        checkbox1.setChecked(mLoudness.getEnabled());
        checkbox1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                //Loudnessを有効・無効にする
                mLoudness.setEnabled(isChecked);
            }
        });

        int gain = (int) mLoudness.getTargetGain();
        mTextPos.setText(Integer.toString(gain));
        textMax.setText(Integer.toString(GAIN_MAX));
        seekbar.setMax(GAIN_MAX);
        seekbar.setProgress(gain);
    }
}
