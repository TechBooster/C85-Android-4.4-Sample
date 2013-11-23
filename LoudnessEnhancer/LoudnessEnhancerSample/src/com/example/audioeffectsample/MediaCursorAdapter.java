package com.example.audioeffectsample;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.provider.MediaStore.Audio;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MediaCursorAdapter extends CursorAdapter {
    private NowPlaying mNowPlaying;
    private LayoutInflater mInflater;
    private MySQLiteOpenHelper mSQLiteOpenHelper;

    public MediaCursorAdapter(Context context, Cursor c, NowPlaying nowplaying) {
        super(context, c, false);
        mInflater = LayoutInflater.from(context);
        mNowPlaying = nowplaying;
        mSQLiteOpenHelper = new MySQLiteOpenHelper(context);
    }

    @Override
    public void bindView(View view, Context arg1, Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));
        String title = cursor.getString(cursor
                .getColumnIndex(Audio.Media.TITLE));
        String artist = cursor.getString(cursor
                .getColumnIndex(Audio.Media.ARTIST));

        TextView textTitle = (TextView) view.findViewById(R.id.textTitle);
        TextView textPeakRMS = (TextView) view.findViewById(R.id.textPeakRms);
        TextView textReplayPeakRms = (TextView) view
                .findViewById(R.id.textReplayPeakRms);

        if (mNowPlaying.getID() == id) {
            view.setBackgroundColor(Color.argb(64, 230, 230, 250));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        textTitle.setText(title + "/" + artist);

        long peak = 0;
        long rms = 0;
        long gain = 0;
        float replay_peak = 0;
        float replay_gain = 0;
        SQLiteDatabase db = mSQLiteOpenHelper.getReadableDatabase();
        if (db != null) {
            Cursor mediaCursor = null;
            try {
                mediaCursor = db.query(Loudness.TABLE, new String[] {
                        Loudness.TRACK_PEAK, Loudness.TRACK_RMS,Loudness.TRACK_GAIN,
                        Loudness.REPLAYGAIN_TRACK_PEAK,
                        Loudness.REPLAYGAIN_TRACK_GAIN }, Loudness.MEDIA_ID
                        + " = ?", new String[] { Integer.toString(id) }, null,
                        null, null);
                if (mediaCursor != null && mediaCursor.moveToFirst()) {
                    peak = mediaCursor.getInt(0);
                    rms = mediaCursor.getInt(1);
                    gain = mediaCursor.getInt(2);
                    replay_peak = mediaCursor.getFloat(3);
                    replay_gain = mediaCursor.getFloat(4);
                }

            } finally {
                if (mediaCursor != null) {
                    mediaCursor.close();
                }
                db.close();
            }
        }
        textPeakRMS.setText(String.format("PEAK:%d RMS:%d GAIN:%d", peak, rms, gain));
        textReplayPeakRms.setText(String
                .format("REPLAY PEAK:%.4f REPLAY GAIN:%.2fdB", replay_peak,
                        replay_gain));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.list_row, parent, false);
        return view;
    }
}
