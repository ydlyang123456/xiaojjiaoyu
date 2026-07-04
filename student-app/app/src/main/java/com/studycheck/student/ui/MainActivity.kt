package com.studycheck.student.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.studycheck.student.App
import com.studycheck.student.R
import com.studycheck.student.databinding.ActivityMainBinding
import com.studycheck.student.ui.home.HomeFragment
import com.studycheck.student.ui.knowledge.KnowledgeActivity
import com.studycheck.student.ui.leaderboard.LeaderboardActivity
import com.studycheck.student.ui.profile.ProfileFragment
import com.studycheck.student.ui.search.SearchFragment
import com.studycheck.student.ui.parent.ParentHomeFragment
import com.studycheck.student.ui.parent.ParentRecordsFragment
import com.studycheck.student.ui.parent.ParentProfileFragment
import com.studycheck.student.ui.analysis.AnalysisActivity
import com.studycheck.student.ui.pet.PetActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var studentHomeFragment: HomeFragment
    private lateinit var searchFragment: SearchFragment
    private lateinit var profileFragment: ProfileFragment
    private lateinit var parentHomeFragment: ParentHomeFragment
    private lateinit var parentRecordsFragment: ParentRecordsFragment
    private lateinit var parentProfileFragment: ParentProfileFragment

    private var activeFragment: Fragment? = null
    private var isParent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isParent = App.instance.prefs.isParent()

        if (isParent) {
            setupParentUI(savedInstanceState)
        } else {
            setupStudentUI(savedInstanceState)
        }

        // 自动检查更新
        Handler(Looper.getMainLooper()).postDelayed({
            UpdateManager(this).checkUpdate()
        }, 2000)
    }

    private fun setupStudentUI(savedInstanceState: Bundle?) {
        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)

        val fm = supportFragmentManager
        if (savedInstanceState == null) {
            studentHomeFragment = HomeFragment()
            searchFragment = SearchFragment()
            profileFragment = ProfileFragment()
            fm.beginTransaction()
                .add(R.id.fragment_container, studentHomeFragment, "home")
                .add(R.id.fragment_container, searchFragment, "search").hide(searchFragment)
                .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                .commit()
        } else {
            studentHomeFragment = fm.findFragmentByTag("home") as HomeFragment
            searchFragment = fm.findFragmentByTag("search") as SearchFragment
            profileFragment = fm.findFragmentByTag("profile") as ProfileFragment
        }
        activeFragment = studentHomeFragment

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(studentHomeFragment)
                    true
                }
                R.id.nav_knowledge -> {
                    startActivity(Intent(this, KnowledgeActivity::class.java))
                    false
                }
                R.id.nav_search -> {
                    switchFragment(searchFragment)
                    true
                }
                R.id.nav_pet -> {
                    startActivity(Intent(this, PetActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    switchFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupParentUI(savedInstanceState: Bundle?) {
        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu_parent)

        val fm = supportFragmentManager
        if (savedInstanceState == null) {
            parentHomeFragment = ParentHomeFragment()
            parentRecordsFragment = ParentRecordsFragment()
            parentProfileFragment = ParentProfileFragment()
            fm.beginTransaction()
                .add(R.id.fragment_container, parentHomeFragment, "parent_home")
                .add(R.id.fragment_container, parentRecordsFragment, "parent_records").hide(parentRecordsFragment)
                .add(R.id.fragment_container, parentProfileFragment, "parent_profile").hide(parentProfileFragment)
                .commit()
        } else {
            parentHomeFragment = fm.findFragmentByTag("parent_home") as ParentHomeFragment
            parentRecordsFragment = fm.findFragmentByTag("parent_records") as ParentRecordsFragment
            parentProfileFragment = fm.findFragmentByTag("parent_profile") as ParentProfileFragment
        }
        activeFragment = parentHomeFragment

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(parentHomeFragment)
                    true
                }
                R.id.nav_records -> {
                    switchFragment(parentRecordsFragment)
                    true
                }
                R.id.nav_analysis -> {
                    val intent = Intent(this, AnalysisActivity::class.java)
                    val studentId = App.instance.prefs.currentStudentId
                    if (studentId > 0) {
                        intent.putExtra("student_id", studentId)
                    }
                    startActivity(intent)
                    false
                }
                R.id.nav_profile -> {
                    switchFragment(parentProfileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(target: Fragment) {
        if (target == activeFragment) return
        val current = activeFragment ?: return
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_slide_up,
                R.anim.fade_slide_down,
                R.anim.fade_slide_up,
                R.anim.fade_slide_down
            )
            .hide(current)
            .show(target)
            .commit()
        activeFragment = target
    }
}
