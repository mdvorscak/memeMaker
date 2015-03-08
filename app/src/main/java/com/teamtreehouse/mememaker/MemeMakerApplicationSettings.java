package com.teamtreehouse.mememaker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.teamtreehouse.mememaker.utils.StorageType;

/**
 * Created by Evan Anger on 8/13/14.
 */
public class MemeMakerApplicationSettings {

    public static final String STORAGE_KEY = "Storage";
    SharedPreferences mSharedPreferences;
    public MemeMakerApplicationSettings(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getStoragePreference(String preferenceKey){
        return mSharedPreferences.getString(preferenceKey, StorageType.INTERNAL);
    }

    public MemeMakerApplicationSettings setSharedPreference(String storageType){
        //commit is synchronous, apply is asynchronous
        mSharedPreferences.edit().putString(STORAGE_KEY, storageType).apply();
        //allow chaining of setting
        return this;
    }
}
