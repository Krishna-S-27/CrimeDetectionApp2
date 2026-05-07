package com.krishna.crimedetection.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Factory for creating CrimeViewModel instances
 * Handles dependency injection for ViewModel
 */
public class CrimeViewModelFactory implements ViewModelProvider.Factory {

    private Application application;

    /**
     * Initialize factory with application context
     *
     * @param application Application instance
     */
    public CrimeViewModelFactory(Application application) {
        this.application = application;
    }

    /**
     * Create new instance of CrimeViewModel
     *
     * @param modelClass ViewModel class to create
     * @param <T> ViewModel type
     * @return New CrimeViewModel instance
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CrimeViewModel.class)) {
            return (T) new CrimeViewModel(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}