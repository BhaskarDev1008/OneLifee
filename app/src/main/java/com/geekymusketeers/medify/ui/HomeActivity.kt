package com.geekymusketeers.medify.ui

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.geekymusketeers.medify.R
import com.geekymusketeers.medify.databinding.ActivityHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityHomeBinding
    private var timer = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        // Hide the action bar
        supportActionBar?.hide()

        val bottomNavigationView = _binding.bottomNav
        val navController: NavController = findNavController(R.id.fragmentContainerView)

        // Configure AppBar with the navigation controller
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.home, R.id.stats, R.id.appointment, R.id.settings))
        bottomNavigationView.setupWithNavController(navController)

        // Add destination changed listener
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.home, R.id.stats, R.id.appointment, R.id.settings -> showBottomNav(bottomNavigationView)
                else -> hideBottomNav(bottomNavigationView)
            }
        }

        // Handle the back press using the OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Custom back navigation logic
                if (navController.currentBackStackEntry?.destination?.id == R.id.home && navController.previousBackStackEntry == null) {
                    if (timer + 2000L > System.currentTimeMillis()) {
                        finish() // Exit the app
                    } else {
                        Toast.makeText(
                            applicationContext, getString(R.string.press_once_again_to_exit),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    timer = System.currentTimeMillis()
                } else {
                    navController.popBackStack() // Pop the back stack
                }
            }
        })
    }

    private fun showBottomNav(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.visibility = View.VISIBLE
    }

    private fun hideBottomNav(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.visibility = View.GONE
    }
}
