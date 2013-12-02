package com.example.audioeffectsample;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 7;

    public MySQLiteOpenHelper(Context context) {
        super(context, "mydatabase.db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(Loudness.TABLE).append(" (");
        sql.append(Loudness._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
        sql.append(",").append(Loudness.TRACK_PEAK).append(" INTEGER");
        sql.append(",").append(Loudness.TRACK_RMS).append(" INTEGER");
        sql.append(",").append(Loudness.TRACK_GAIN).append(" INTEGER");
        sql.append(",").append(Loudness.REPLAYGAIN_TRACK_PEAK).append(" FLOAT");
        sql.append(",").append(Loudness.REPLAYGAIN_TRACK_GAIN).append(" FLOAT");
        sql.append(",").append(Loudness.MEDIA_ID).append(" INTEGER");
        sql.append(");");
        db.execSQL(sql.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        db.execSQL("DROP TABLE IF EXISTS " + Loudness.TABLE);
        onCreate(db);
    }

}
