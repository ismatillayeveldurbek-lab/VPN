package uz.tulpor.vpn.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Builds Xray-core JSON configs. The shape is covered by core/testdata/vpn_config.json,
 * which the Go tests load into Xray.
 */
object ConfigBuilder {
    const val TUN_MTU = 1500
    val DNS_SERVERS = listOf("1.1.1.1", "8.8.8.8")
    const val PING_URL = "https://www.gstatic.com/generate_204"

    private val PRIVATE_NETS = listOf(
        "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8",
        "169.254.0.0/16", "100.64.0.0/10", "fc00::/7", "fe80::/10",
    )

    data class Options(
        /** Send *.uz sites directly (they are not blocked and are faster without a tunnel). */
        val bypassUz: Boolean = true,
        val logLevel: String = "warning",
    )

    /** Full config for the VPN: TUN inbound -> VLESS proxy, with DNS and bypass rules. */
    fun vpnConfig(server: Server, options: Options = Options()): String = buildJsonObject {
        putJsonObject("log") { put("loglevel", options.logLevel) }
        putJsonObject("dns") {
            put("servers", JsonArray(DNS_SERVERS.map(::JsonPrimitive)))
            put("queryStrategy", "UseIPv4")
        }
        putJsonArray("inbounds") {
            add(buildJsonObject {
                put("tag", "tun")
                put("port", 0)
                put("protocol", "tun")
                putJsonObject("settings") {
                    put("name", "tulpor0")
                    put("MTU", TUN_MTU)
                }
                putJsonObject("sniffing") {
                    put("enabled", true)
                    put("destOverride", JsonArray(listOf("http", "tls", "quic").map(::JsonPrimitive)))
                    put("routeOnly", true)
                }
            })
        }
        putJsonArray("outbounds") {
            add(proxyOutbound(server))
            add(buildJsonObject {
                put("tag", "direct"); put("protocol", "freedom")
                putJsonObject("settings") { put("domainStrategy", "UseIPv4") }
            })
            add(buildJsonObject { put("tag", "block"); put("protocol", "blackhole") })
            add(buildJsonObject { put("tag", "dns-out"); put("protocol", "dns") })
        }
        putJsonObject("routing") {
            put("domainStrategy", "AsIs")
            putJsonArray("rules") {
                add(rule("dns-out") {
                    put("inboundTag", JsonArray(listOf(JsonPrimitive("tun"))))
                    put("port", "53")
                })
                add(rule("direct") { put("ip", JsonArray(PRIVATE_NETS.map(::JsonPrimitive))) })
                if (options.bypassUz) {
                    add(rule("direct") { put("domain", JsonArray(listOf(JsonPrimitive("domain:uz")))) })
                }
            }
        }
    }.toString()

    /** Outbound-only config used to measure real latency through the server. */
    fun pingConfig(server: Server): String = buildJsonObject {
        putJsonObject("log") { put("loglevel", "none") }
        putJsonArray("outbounds") { add(proxyOutbound(server)) }
    }.toString()

    fun proxyOutbound(s: Server): JsonObject = buildJsonObject {
        put("tag", "proxy")
        put("protocol", "vless")
        putJsonObject("settings") {
            putJsonArray("vnext") {
                add(buildJsonObject {
                    put("address", s.address)
                    put("port", s.port)
                    putJsonArray("users") {
                        add(buildJsonObject {
                            put("id", s.uuid)
                            put("encryption", "none")
                            if (s.flow.isNotBlank() && s.network == "tcp") put("flow", s.flow)
                        })
                    }
                })
            }
        }
        put("streamSettings", streamSettings(s))
        putJsonObject("mux") { put("enabled", false) }
    }

    private fun streamSettings(s: Server): JsonObject = buildJsonObject {
        put("network", s.network)
        put("security", s.security)
        when (s.security) {
            "reality" -> putJsonObject("realitySettings") {
                put("serverName", s.sni)
                put("fingerprint", s.fingerprint.ifBlank { "chrome" })
                put("publicKey", s.publicKey)
                put("shortId", s.shortId)
                put("spiderX", s.spiderX.ifBlank { "/" })
            }
            "tls" -> putJsonObject("tlsSettings") {
                put("serverName", s.sni.ifBlank { s.host.ifBlank { s.address } })
                put("fingerprint", s.fingerprint.ifBlank { "chrome" })
                if (s.alpn.isNotBlank()) {
                    put("alpn", JsonArray(s.alpn.split(',').map { JsonPrimitive(it.trim()) }))
                }
            }
        }
        when (s.network) {
            "ws" -> putJsonObject("wsSettings") {
                put("path", s.path.ifBlank { "/" })
                if (s.host.isNotBlank()) put("host", s.host)
            }
            "grpc" -> putJsonObject("grpcSettings") {
                put("serviceName", s.serviceName)
                put("multiMode", s.mode == "multi")
            }
            "xhttp", "splithttp" -> putJsonObject("xhttpSettings") {
                put("path", s.path.ifBlank { "/" })
                if (s.host.isNotBlank()) put("host", s.host)
                put("mode", s.mode.ifBlank { "auto" })
            }
            "httpupgrade" -> putJsonObject("httpupgradeSettings") {
                put("path", s.path.ifBlank { "/" })
                if (s.host.isNotBlank()) put("host", s.host)
            }
            "tcp" -> if (s.headerType == "http") putJsonObject("tcpSettings") {
                putJsonObject("header") { put("type", "http") }
            }
        }
    }

    private fun rule(outbound: String, body: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) =
        buildJsonObject {
            put("type", "field")
            body()
            put("outboundTag", outbound)
        }
}
