package com.studycheck.student.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)
        val appName = findViewById<TextView>(R.id.appName)

        val scaleAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo)
        val fadeAnim = AnimationUtils.loadAnimation(this, R.anim.splash_text)

        logo.startAnimation(scaleAnim)
        appName.startAnimation(fadeAnim)

        Handler(Looper.getMainLooper()).postDelayed({
            checkPrivacyAndProceed()
        }, 1800)
    }

    private fun checkPrivacyAndProceed() {
        if (App.instance.prefs.privacyAgreed) {
            checkLogin()
        } else {
            showPrivacyDialog()
        }
    }

    private fun showPrivacyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_privacy, null)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("同意") { _, _ ->
                App.instance.prefs.privacyAgreed = true
                checkLogin()
            }
            .setNegativeButton("不同意") { _, _ ->
                finish()
            }
            .show()
    }

    private fun checkLogin() {
        if (App.instance.prefs.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        finish()
    }
}
