package com.studycheck.parent.data

import android.content.SharedPreferences

class PreferenceManager(private val prefs: SharedPreferences) {

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, 0)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var nickname: String?
        get() = prefs.getString(KEY_NICKNAME, null)
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var currentStudentId: Int
        get() = prefs.getInt(KEY_CURRENT_STUDENT_ID, 0)
        set(value) = prefs.edit().putInt(KEY_CURRENT_STUDENT_ID, value).apply()

    var currentStudentName: String?
        get() = prefs.getString(KEY_CURRENT_STUDENT_NAME, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_STUDENT_NAME, value).apply()

    fun isLoggedIn(): Boolean = !token.isNullOrEmpty()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun saveUser(user: User) {
        userId = user.id
        username = user.username
        nickname = user.nickname
        role = user.role
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_ROLE = "role"
        private const val KEY_CURRENT_STUDENT_ID = "current_student_id"
        private const val KEY_CURRENT_STUDENT_NAME = "current_student_name"
    }
}
