package com.studycheck.student

import android.app.Application
import android.content.Context
import com.studycheck.student.data.PreferenceManager

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    lateinit var prefs: PreferenceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferenceManager(getSharedPreferences("studycheck", Context.MODE_PRIVATE))
    }
}
