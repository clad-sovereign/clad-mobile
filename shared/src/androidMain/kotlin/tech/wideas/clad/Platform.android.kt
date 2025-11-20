package tech.wideas.clad

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val defaultRpcEndpoint: String = "ws://10.0.2.2:9944"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()