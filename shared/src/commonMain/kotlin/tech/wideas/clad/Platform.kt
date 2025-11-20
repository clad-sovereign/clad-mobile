package tech.wideas.clad

interface Platform {
    val name: String
    val defaultRpcEndpoint: String
}

expect fun getPlatform(): Platform

expect fun currentTimeMillis(): Long