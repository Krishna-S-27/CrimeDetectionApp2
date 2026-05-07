package com.krishna.crimedetection.viewmodel;

import androidx.lifecycle.Observer;
import android.util.Log;

/**
 * Base observer for LiveData with logging
 * Extends Observer to add logging and error handling
 *
 * @param <T> Type of data to observe
 */
public abstract class LiveDataObserver<T> implements Observer<T> {

    private String tag;

    /**
     * Initialize observer with logging tag
     *
     * @param tag Log tag for debugging
     */
    public LiveDataObserver(String tag) {
        this.tag = tag;
    }

    @Override
    public void onChanged(T data) {
        try {
            if (data == null) {
                Log.d(tag, "LiveData changed: null");
                onDataNull();
            } else {
                Log.d(tag, "LiveData changed: " + data.getClass().getSimpleName());
                onDataChanged(data);
            }
        } catch (Exception e) {
            Log.e(tag, "Error in LiveData observer: " + e.getMessage());
            onError(e);
        }
    }

    /**
     * Called when data changes and is not null
     *
     * @param data The new data
     */
    public abstract void onDataChanged(T data);

    /**
     * Called when data is null
     */
    public void onDataNull() {
        // Override if needed
    }

    /**
     * Called when error occurs
     *
     * @param exception The exception
     */
    public void onError(Exception exception) {
        // Override if needed
        exception.printStackTrace();
    }
}