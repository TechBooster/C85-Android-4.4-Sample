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

        TextView textTitle = (TextView) view.findViewById(R.id.textTitle);
        TextView textMaxPeak = (TextView) view.findViewById(R.id.textMaxPeak);
        TextView textMaxRMS = (TextView) view.findViewById(R.id.textMaxRMS);

        if (mNowPlaying.getID() == id) {
            view.setBackgroundColor(Color.argb(64, 230, 230, 250));
        }else{
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        textTitle.setText(title);
        textMaxPeak.setText("");
        textMaxRMS.setText("");

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
                    long peakMax = mediaCursor.getInt(0);
                    long rmsMax = mediaCursor.getInt(1);
                    textMaxPeak.setText("PEAK MAX:" + peakMax);
                    textMaxRMS.setText("RMS MAX:" + rmsMax);
                }

            } finally {
                if (mediaCursor != null) {
                    mediaCursor.close();
                }
                db.close();
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.list_row, parent, false);
        return view;
    }
}
