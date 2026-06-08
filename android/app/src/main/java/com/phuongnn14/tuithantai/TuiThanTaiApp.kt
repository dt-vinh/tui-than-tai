package com.phuongnn14.tuithantai

import android.app.Application
import com.phuongnn14.tuithantai.data.AppDatabase

class TuiThanTaiApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
