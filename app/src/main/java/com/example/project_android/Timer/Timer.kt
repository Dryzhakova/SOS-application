package com.example.project_android.Timer

import android.os.CountDownTimer

class Timer(private val onTimerComplete: () -> Unit) {

    private var countDownTimer: CountDownTimer? = null

    fun startTimer(milliseconds: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                onTimerComplete()
            }
        }.start()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
    }
}
