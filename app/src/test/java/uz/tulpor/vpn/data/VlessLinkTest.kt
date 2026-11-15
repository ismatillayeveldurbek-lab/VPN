package uz.tulpor.vpn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VlessLinkTest {
    private val reality = "vless://b831381d-6324-4d53-ad4f-8cda48b30811@203.0.113.10:443" +
        "?encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.microsoft.com&fp=chrome" +
        "&pbk=Z84J2IelR9ch3k8VtlVhhs5ycBUlXA7wHBWcBrjqnAw&sid=6ba85179e30d4fc2&type=tcp#DE-Frankfurt"

    @Test fun parsesRealityLink() {
        val s = VlessLink.parse(reality)!!
        assertEquals("203.0.113.10", s.address)
        assertEquals(443, s.port)
        assertEquals("b831381d-6324-4d53-ad4f-8cda48b30811", s.uuid)
        assertEquals("xtls-rprx-vision", s.flow)
        assertEquals("reality", s.security)
        assertEquals("www.microsoft.com", s.sni)
        assertEquals("Z84J2IelR9ch3k8VtlVhhs5ycBUlXA7wHBWcBrjqnAw", s.publicKey)
        assertEquals("6ba85179e30d4fc2", s.shortId)
        assertEquals("tcp", s.network)
        assertEquals("DE", s.country)
        assertEquals("Frankfurt", s.city)
    }

    @Test fun roundTrips() {
        val s = VlessLink.parse(reality)!!
        val again = VlessLink.parse(VlessLink.format(s))!!
        assertEquals(s.copy(id = again.id), again)
    }

    @Test fun parsesIpv6AndEncodedRemark() {
        val s = VlessLink.parse("vless://id@[2001:db8::1]:8443?type=ws&security=tls&path=%2Fws&host=cdn.example.com#%F0%9F%87%B3%F0%9F%87%B1%20Amsterdam")!!
        assertEquals("2001:db8::1", s.address)
        assertEquals(8443, s.port)
        assertEquals("ws", s.network)
        assertEquals("/ws", s.path)
        assertEquals("NL", s.country)
        assertEquals("Amsterdam", s.city)
    }

    @Test fun rejectsGarbage() {
        assertNull(VlessLink.parse("vmess://abc"))
        assertNull(VlessLink.parse("vless://nohost"))
        assertNull(VlessLink.parse("vless://id@host:99999"))
    }

    @Test fun parseAllFindsSeveralAndDeduplicates() {
        val text = "hello\n$reality\n$reality  vless://x@1.2.3.4:443?security=none#Finland"
        val list = VlessLink.parseAll(text)
        assertEquals(2, list.size)
        assertEquals("FI", list[1].country)
    }

    @Test fun detectsCountryByName() {
        assertEquals("DE" to "Frankfurt", Countries.detect("Germaniya Frankfurt"))
        assertEquals("NL", Countries.detect("Нидерланды").first)
        assertEquals("" to "My server", Countries.detect("My server"))
        assertNotNull(Countries.displayName("FI"))
    }
}
