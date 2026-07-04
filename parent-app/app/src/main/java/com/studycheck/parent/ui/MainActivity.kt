package com.studycheck.parent.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.studycheck.parent.R
import com.studycheck.parent.databinding.ActivityMainBinding
import com.studycheck.parent.ui.analysis.AnalysisActivity
import com.studycheck.parent.ui.auth.LoginActivity
import com.studycheck.parent.ui.home.HomeFragment
import com.studycheck.parent.ui.records.RecordsFragment
import com.studycheck.parent.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_records -> {
                    replaceFragment(RecordsFragment())
                    true
                }
                R.id.nav_analysis -> {
                    startActivity(Intent(this, AnalysisActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
