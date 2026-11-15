package uz.tulpor.vpn.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConfigBuilderTest {
    private val server = VlessLink.parse(
        "vless://b831381d-6324-4d53-ad4f-8cda48b30811@203.0.113.10:443?flow=xtls-rprx-vision&security=reality" +
            "&sni=www.microsoft.com&fp=chrome&pbk=Z84J2IelR9ch3k8VtlVhhs5ycBUlXA7wHBWcBrjqnAw&sid=6ba85179e30d4fc2&type=tcp#DE"
    )!!

    @Test fun vpnConfigHasTunInboundAndRealityOutbound() {
        val json = ConfigBuilder.vpnConfig(server)
        // Exported so core/ Go tests can load the exact config into Xray (see core_test.go).
        File("build/generated-vpn-config.json").apply { parentFile?.mkdirs() }.writeText(json)

        val root = Json.parseToJsonElement(json).jsonObject
        val inbound = root["inbounds"]!!.jsonArray[0].jsonObject
        assertEquals("tun", inbound["protocol"]!!.jsonPrimitive.content)
        val out = root["outbounds"]!!.jsonArray[0].jsonObject
        assertEquals("vless", out["protocol"]!!.jsonPrimitive.content)
        val reality = out["streamSettings"]!!.jsonObject["realitySettings"]!!.jsonObject
        assertEquals("Z84J2IelR9ch3k8VtlVhhs5ycBUlXA7wHBWcBrjqnAw", reality["publicKey"]!!.jsonPrimitive.content)
        assertTrue(json.contains("domain:uz"))
    }

    @Test fun bypassUzCanBeDisabled() {
        assertFalse(ConfigBuilder.vpnConfig(server, ConfigBuilder.Options(bypassUz = false)).contains("domain:uz"))
    }

    @Test fun visionFlowOnlyOnTcp() {
        val ws = server.copy(network = "ws", security = "tls")
        assertFalse(ConfigBuilder.pingConfig(ws).contains("xtls-rprx-vision"))
        assertTrue(ConfigBuilder.pingConfig(server).contains("xtls-rprx-vision"))
    }
}
