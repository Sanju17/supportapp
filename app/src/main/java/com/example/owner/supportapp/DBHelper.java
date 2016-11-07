package com.example.owner.supportapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "NotificationDB.db";
    public static final String NOTIFICATION_TABLE_NAME = "notification";
    public static final String NOTIFICATION_COLUMN_ID = "_id";
    public static final String NOTIFICATION_COLUMN_DATE = "date";
    public static final String NOTIFICATION_COLUMN_MESSAGE = "message";

    public DBHelper(Context context){
        super(context,DATABASE_NAME, null, 1);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table " + NOTIFICATION_TABLE_NAME +
                        "("
                        + NOTIFICATION_COLUMN_ID + " integer primary key autoincrement, "
                        + NOTIFICATION_COLUMN_DATE + " text, "
                        + NOTIFICATION_COLUMN_MESSAGE +" text " +
                        ")"
        );
    }

    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS "+ NOTIFICATION_TABLE_NAME);
        onCreate(database);
    }

    public void insertContact(String message, String date)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(NOTIFICATION_COLUMN_MESSAGE, message);
        contentValues.put(NOTIFICATION_COLUMN_DATE, date);
        db.insert(NOTIFICATION_TABLE_NAME, null, contentValues);
    }

    public Cursor getData(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from " + NOTIFICATION_TABLE_NAME + " order by " + NOTIFICATION_COLUMN_ID + " DESC", null );
        return res;
    }

    public void deleteUnnecessaryData(){
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DELETE FROM "+ NOTIFICATION_TABLE_NAME +" WHERE "+ NOTIFICATION_COLUMN_ID +" NOT IN (SELECT " + NOTIFICATION_COLUMN_ID + " FROM " + NOTIFICATION_TABLE_NAME + " ORDER BY " + NOTIFICATION_COLUMN_ID + " DESC LIMIT 100)";
        db.execSQL(sql);
    }
}