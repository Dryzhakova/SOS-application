package com.example.project_android.ViewModel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _isTimerStarted = MutableLiveData<Boolean>()
    val isTimerStarted: LiveData<Boolean> get() = _isTimerStarted

    private val _isGPSActive = MutableLiveData<Boolean>()
    val isGPSActive: LiveData<Boolean> get() = _isGPSActive

    private val _isAccelerometerActive = MutableLiveData<Boolean>()
    val isAccelerometerActive: LiveData<Boolean> get() = _isAccelerometerActive

    fun setTimerStarted(value: Boolean) {
        _isTimerStarted.value = value
    }

    fun setGPSActive(value: Boolean) {
        _isGPSActive.value = value
    }

    fun setAccelerometerActive(value: Boolean) {
        _isAccelerometerActive.value = value
    }
}