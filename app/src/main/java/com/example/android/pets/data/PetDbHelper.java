package com.example.android.pets.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.android.pets.data.PetContract.PetEntry;

public class PetDbHelper extends SQLiteOpenHelper {

    // Database version.
    private static final String DATABASE_NAME = "shelter.db";

    // Database name.
    private static final int DATABASE_VERSION = 1;

    // Constructor.
    public PetDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PETS_TABLE = "CREATE TABLE " + PetEntry.TABLE_NAME + "(" +
                PetEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PetEntry.COLUMN_PET_NAME + " TEXT, " +
                PetEntry.COLUMN_PET_BREED + " TEXT, " +
                PetEntry.COLUMN_PET_GENDER + " INTEGER, " +
                PetEntry.COLUMN_PET_WEIGHT + " INTEGER" + ");";

        db.execSQL(CREATE_PETS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: Leave blank for now.
    }
}
