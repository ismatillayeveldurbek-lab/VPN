package uz.tulpor.vpn.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

/** Parses and formats `vless://` share links (Xray / v2rayN format). */
object VlessLink {

    fun parse(link: String, source: String = Server.SOURCE_USER): Server? {
        val s = link.trim()
        if (!s.startsWith("vless://", ignoreCase = true)) return null
        var rest = s.substring("vless://".length)

        val remark = rest.substringAfter('#', "").let { decode(it) }
        rest = rest.substringBefore('#')
        val query = rest.substringAfter('?', "")
        rest = rest.substringBefore('?').trimEnd('/')

        val at = rest.lastIndexOf('@')
        if (at <= 0) return null
        val uuid = decode(rest.substring(0, at))
        val hostPort = rest.substring(at + 1)

        val host: String
        val portStr: String
        if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            if (end < 0) return null
            host = hostPort.substring(1, end)
            portStr = hostPort.substring(end + 1).removePrefix(":")
        } else {
            host = hostPort.substringBeforeLast(':')
            portStr = hostPort.substringAfterLast(':', "")
        }
        val port = portStr.toIntOrNull() ?: return null
        if (host.isBlank() || port !in 1..65535 || uuid.isBlank()) return null

        val p = parseQuery(query)
        val network = p["type"]?.lowercase()?.let { if (it == "raw") "tcp" else it } ?: "tcp"
        val (country, city) = Countries.detect(remark)

        return Server(
            id = UUID.nameUUIDFromBytes(s.substringBefore('#').toByteArray()).toString(),
            name = remark.ifBlank { host },
            country = country,
            city = city,
            address = host,
            port = port,
            uuid = uuid,
            flow = p["flow"].orEmpty(),
            security = p["security"]?.lowercase() ?: "none",
            sni = p["sni"] ?: p["peer"].orEmpty(),
            fingerprint = p["fp"]?.ifBlank { null } ?: "chrome",
            publicKey = p["pbk"].orEmpty(),
            shortId = p["sid"].orEmpty(),
            spiderX = p["spx"].orEmpty(),
            alpn = p["alpn"].orEmpty(),
            network = network,
            path = p["path"].orEmpty(),
            host = p["host"].orEmpty(),
            serviceName = p["serviceName"].orEmpty(),
            mode = p["mode"].orEmpty(),
            headerType = p["headerType"].orEmpty(),
            source = source,
        )
    }

    /** Parses every vless:// link found in [text] (one per line, or whitespace separated). */
    fun parseAll(text: String, source: String = Server.SOURCE_USER): List<Server> =
        text.split(Regex("\\s+"))
            .mapNotNull { parse(it, source) }
            .distinctBy { it.id }

    fun format(server: Server): String {
        val params = linkedMapOf(
            "encryption" to "none",
            "type" to server.network,
            "security" to server.security,
            "flow" to server.flow,
            "sni" to server.sni,
            "fp" to server.fingerprint,
            "pbk" to server.publicKey,
            "sid" to server.shortId,
            "spx" to server.spiderX,
            "alpn" to server.alpn,
            "path" to server.path,
            "host" to server.host,
            "serviceName" to server.serviceName,
            "mode" to server.mode,
            "headerType" to server.headerType,
        ).filterValues { it.isNotBlank() }
        val q = params.entries.joinToString("&") { "${it.key}=${encode(it.value)}" }
        val h = if (server.address.contains(':')) "[${server.address}]" else server.address
        return "vless://${server.uuid}@$h:${server.port}?$q#${encode(server.name)}"
    }

    private fun parseQuery(q: String): Map<String, String> =
        q.split('&').filter { it.isNotBlank() }.associate {
            val k = it.substringBefore('=')
            val v = it.substringAfter('=', "")
            decode(k) to decode(v)
        }

    private fun decode(s: String): String = try {
        URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
    } catch (_: IllegalArgumentException) {
        s
    }

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
