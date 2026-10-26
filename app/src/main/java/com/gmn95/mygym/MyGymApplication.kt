package com.gmn95.mygym

import android.app.Application
import com.gmn95.mygym.data.local.MyGymDatabase

class MyGymApplication : Application() {
    val database: MyGymDatabase by lazy {
        MyGymDatabase.create(applicationContext)
    }
}
