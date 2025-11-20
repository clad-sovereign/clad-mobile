package tech.wideas.clad

import platform.UIKit.UIDevice
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val defaultRpcEndpoint: String = "ws://127.0.0.1:9944"
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()