package com.studycheck.parent

import android.app.Application
import android.content.Context
import com.studycheck.parent.data.PreferenceManager

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
        prefs = PreferenceManager(getSharedPreferences("studycheck_parent", Context.MODE_PRIVATE))
    }
}
