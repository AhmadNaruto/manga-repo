package eu.kanade.tachiyomi.extension.id.aarlas

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga
import okhttp3.Dns
import java.net.Inet4Address

class Aarlas : ZeistManga("Aarlas", "https://www.arlas.my.id", "id") {

    // Enforce IPv4 connections by filtering DNS lookups
    private val ipv4Dns by lazy {
        Dns { hostname ->
            Dns.SYSTEM.lookup(hostname).filter { it is Inet4Address }
        }
    }

    override val client = network.cloudflareClient.newBuilder()
        .dns(ipv4Dns)
        .build()
}
