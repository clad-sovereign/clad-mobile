package tech.wideas.clad

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform