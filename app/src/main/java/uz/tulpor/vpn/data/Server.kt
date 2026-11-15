package uz.tulpor.vpn.data

import kotlinx.serialization.Serializable

/** A VLESS endpoint. Field names mirror the vless:// share-link parameters. */
@Serializable
data class Server(
    val id: String,
    val name: String,
    val country: String = "",
    val city: String = "",
    val address: String,
    val port: Int,
    val uuid: String,
    val flow: String = "",
    val security: String = "none",
    val sni: String = "",
    val fingerprint: String = "chrome",
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    val alpn: String = "",
    val network: String = "tcp",
    val path: String = "",
    val host: String = "",
    val serviceName: String = "",
    val mode: String = "",
    val headerType: String = "",
    val premium: Boolean = false,
    val source: String = SOURCE_USER,
) {
    companion object {
        const val SOURCE_USER = "user"
        const val SOURCE_SUBSCRIPTION = "sub"
        const val SOURCE_BUILTIN = "builtin"
    }
}
