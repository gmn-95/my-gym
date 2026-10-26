package com.gmn95.mygym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.gmn95.mygym.ui.MyGymApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyGymApp()
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            (application as MyGymApplication).sessionRepository.checkpointActiveTimers()
        }
    }
}
