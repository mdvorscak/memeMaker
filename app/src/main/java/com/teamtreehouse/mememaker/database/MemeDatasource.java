package com.teamtreehouse.mememaker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.teamtreehouse.mememaker.models.Meme;
import com.teamtreehouse.mememaker.models.MemeAnnotation;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Evan Anger on 8/17/14.
 */
public class MemeDatasource {

    private Context mContext;
    private MemeSQLiteHelper mMemeSqlLiteHelper;

    public MemeDatasource(Context context) {
        mContext = context;
        mMemeSqlLiteHelper = new MemeSQLiteHelper(context);
    }

    private SQLiteDatabase open(){
        return mMemeSqlLiteHelper.getWritableDatabase();
    }

    private void close(SQLiteDatabase database){
        database.close();
    }

    public ArrayList<Meme> read(){
        ArrayList<Meme> memes = readMemes();
        addMemeAnnotations(memes);
        return memes;
    }

    public ArrayList<Meme> readMemes(){
        SQLiteDatabase db = open();

        Cursor cursor = db.query(MemeSQLiteHelper.MEMES_TABLE,
                new String [] {MemeSQLiteHelper.COLUMN_MEME_NAME, BaseColumns._ID, MemeSQLiteHelper.COLUMN_MEME_ASSET},
                null, //selection
                null, //selection args
                null, //groupby
                null, //having
                null); //order

        ArrayList<Meme> memes = new ArrayList<>();
        if(cursor.moveToFirst()){
            do{
                Meme meme = new Meme(getIntFromColumnName(cursor, BaseColumns._ID),
                                     getStringFromColumnName(cursor, MemeSQLiteHelper.COLUMN_MEME_ASSET),
                                     getStringFromColumnName(cursor, MemeSQLiteHelper.COLUMN_MEME_NAME),
                                     null);
                memes.add(meme);
            } while(cursor.moveToNext());
        }
        cursor.close();
        close(db);
        return memes;
    }

    public void addMemeAnnotations(ArrayList<Meme> memes){
        SQLiteDatabase db = open();

        for(Meme meme : memes){
            ArrayList<MemeAnnotation> annotations = new ArrayList<>();
            Cursor cursor = db.rawQuery(
              "SELECT * FROM " + MemeSQLiteHelper.ANNOTATIONS_TABLE +
              " WHERE MEME_ID = " + meme.getId(), null);

            if(cursor.moveToFirst()){
                do{
                    MemeAnnotation annotation = new MemeAnnotation(
                        getIntFromColumnName(cursor, BaseColumns._ID),
                        getStringFromColumnName(cursor, MemeSQLiteHelper.COLUMN_ANNOTATION_COLOR),
                        getStringFromColumnName(cursor, MemeSQLiteHelper.COLUMN_ANNOTATION_TITLE),
                        getIntFromColumnName(cursor, MemeSQLiteHelper.COLUMN_ANNOTATION_X),
                        getIntFromColumnName(cursor, MemeSQLiteHelper.COLUMN_ANNOTATION_Y)
                    );
                    annotations.add(annotation);
                }while(cursor.moveToNext());
            }
            meme.setAnnotations(annotations);
            cursor.close();
            close(db);
        }
    }

    public void update(Meme meme){
        SQLiteDatabase db = open();
        db.beginTransaction();

        ContentValues updateMemeValues = new ContentValues();
        updateMemeValues.put(MemeSQLiteHelper.COLUMN_MEME_NAME, meme.getName());
        db.update(MemeSQLiteHelper.MEMES_TABLE,
                updateMemeValues,
                String.format("%s=%d", BaseColumns._ID, meme.getId()), null); //where clause

        for(MemeAnnotation annotation : meme.getAnnotations()){
            ContentValues updateAnnotations = new ContentValues();
            updateAnnotations.put(MemeSQLiteHelper.COLUMN_ANNOTATION_TITLE, annotation.getTitle());
            updateAnnotations.put(MemeSQLiteHelper.COLUMN_ANNOTATION_X, annotation.getLocationX());
            updateAnnotations.put(MemeSQLiteHelper.COLUMN_ANNOTATION_Y, annotation.getLocationY());
            updateAnnotations.put(MemeSQLiteHelper.COLUMN_FOREIGN_KEY_MEME, meme.getId());
            updateAnnotations.put(MemeSQLiteHelper.COLUMN_ANNOTATION_COLOR, annotation.getColor());

            if(annotation.hasBeenSaved()){
                db.update(MemeSQLiteHelper.ANNOTATIONS_TABLE,
                        updateAnnotations,
                        String.format("%s=%d", BaseColumns._ID, annotation.getId()),
                        null);
            } else {
                //create the annotation instead
                db.insert(MemeSQLiteHelper.ANNOTATIONS_TABLE,
                          null,
                          updateAnnotations);
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        close(db);
    }

    public void delete(int memeId){
        SQLiteDatabase db = open();
        db.beginTransaction();

        db.delete(MemeSQLiteHelper.ANNOTATIONS_TABLE,
                 String.format("%s=%s", MemeSQLiteHelper.COLUMN_FOREIGN_KEY_MEME, String.valueOf(memeId)),
                 null);

        db.delete(MemeSQLiteHelper.MEMES_TABLE,
                 String.format("%s=%s", BaseColumns._ID, String.valueOf(memeId)),
                 null);

        db.setTransactionSuccessful();
        db.endTransaction();
        close(db);
    }

    private int getIntFromColumnName(Cursor cursor, String columnName){
        int columnIndex = cursor.getColumnIndex(columnName);
        return cursor.getInt(columnIndex);
    }

    private String getStringFromColumnName(Cursor cursor, String columnName){
        int columnIndex = cursor.getColumnIndex(columnName);
        return cursor.getString(columnIndex);
    }

    public void create(Meme meme){
        SQLiteDatabase db = open();
        db.beginTransaction();

        ContentValues memeValues = new ContentValues();
        memeValues.put(MemeSQLiteHelper.COLUMN_MEME_NAME, meme.getName());
        memeValues.put(MemeSQLiteHelper.COLUMN_MEME_ASSET, meme.getAssetLocation());
        long memeID = db.insert(MemeSQLiteHelper.MEMES_TABLE, null, memeValues);

        for(MemeAnnotation annotation: meme.getAnnotations()){
            ContentValues annotationValues = new ContentValues();
            annotationValues.put(MemeSQLiteHelper.COLUMN_ANNOTATION_COLOR, annotation.getColor());
            annotationValues.put(MemeSQLiteHelper.COLUMN_ANNOTATION_TITLE, annotation.getTitle());
            annotationValues.put(MemeSQLiteHelper.COLUMN_ANNOTATION_X, annotation.getLocationX());
            annotationValues.put(MemeSQLiteHelper.COLUMN_ANNOTATION_Y, annotation.getLocationY());
            annotationValues.put(MemeSQLiteHelper.COLUMN_FOREIGN_KEY_MEME, memeID);

            db.insert(MemeSQLiteHelper.ANNOTATIONS_TABLE, null, annotationValues);
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        close(db);
    }
}
