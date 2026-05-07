package com.krishna.crimedetection.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Kotlin extension functions for easier LiveData observation
 * Use these for cleaner code in Kotlin activities/fragments
 */

/**
 * Observe LiveData with lambda function
 * Usage: viewModel.prediction.observe(this) { data -> ... }
 */
fun <T> LiveData<T>.observe(owner: LifecycleOwner, action: (T) -> Unit) {
    observe(owner, Observer { action(it) })
}

/**
 * Observe non-null LiveData
 * Usage: viewModel.prediction.observeNonNull(this) { data -> ... }
 */
fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, action: (T) -> Unit) {
    observe(owner) { it?.let { data -> action(data) } }
}