package com.kiroku.app

import android.app.Application

class KirokuApplication : Application() {
    val appContainer: AppContainer by lazy { DefaultAppContainer(this) }
}
