package uz.tulpor.vpn.data

import java.util.Locale

/** Country names and detection of a country from a server's display name. */
object Countries {
    private data class Names(val uz: String, val ru: String, val en: String)

    private val names: Map<String, Names> = mapOf(
        "DE" to Names("Germaniya", "Германия", "Germany"),
        "NL" to Names("Niderlandiya", "Нидерланды", "Netherlands"),
        "FI" to Names("Finlandiya", "Финляндия", "Finland"),
        "FR" to Names("Fransiya", "Франция", "France"),
        "PL" to Names("Polsha", "Польша", "Poland"),
        "EE" to Names("Estoniya", "Эстония", "Estonia"),
        "LV" to Names("Latviya", "Латвия", "Latvia"),
        "LT" to Names("Litva", "Литва", "Lithuania"),
        "AT" to Names("Avstriya", "Австрия", "Austria"),
        "CH" to Names("Shveytsariya", "Швейцария", "Switzerland"),
        "SE" to Names("Shvetsiya", "Швеция", "Sweden"),
        "NO" to Names("Norvegiya", "Норвегия", "Norway"),
        "DK" to Names("Daniya", "Дания", "Denmark"),
        "GB" to Names("Buyuk Britaniya", "Великобритания", "United Kingdom"),
        "IE" to Names("Irlandiya", "Ирландия", "Ireland"),
        "IT" to Names("Italiya", "Италия", "Italy"),
        "ES" to Names("Ispaniya", "Испания", "Spain"),
        "BE" to Names("Belgiya", "Бельгия", "Belgium"),
        "RO" to Names("Ruminiya", "Румыния", "Romania"),
        "BG" to Names("Bolgariya", "Болгария", "Bulgaria"),
        "HU" to Names("Vengriya", "Венгрия", "Hungary"),
        "CZ" to Names("Chexiya", "Чехия", "Czechia"),
        "TR" to Names("Turkiya", "Турция", "Turkey"),
        "GE" to Names("Gruziya", "Грузия", "Georgia"),
        "AM" to Names("Armaniston", "Армения", "Armenia"),
        "AZ" to Names("Ozarbayjon", "Азербайджан", "Azerbaijan"),
        "KZ" to Names("Qozog'iston", "Казахстан", "Kazakhstan"),
        "KG" to Names("Qirg'iziston", "Киргизия", "Kyrgyzstan"),
        "UZ" to Names("O'zbekiston", "Узбекистан", "Uzbekistan"),
        "RU" to Names("Rossiya", "Россия", "Russia"),
        "AE" to Names("BAA", "ОАЭ", "UAE"),
        "US" to Names("AQSh", "США", "United States"),
        "CA" to Names("Kanada", "Канада", "Canada"),
        "JP" to Names("Yaponiya", "Япония", "Japan"),
        "SG" to Names("Singapur", "Сингапур", "Singapore"),
        "HK" to Names("Gonkong", "Гонконг", "Hong Kong"),
        "IN" to Names("Hindiston", "Индия", "India"),
    )

    val codes: Set<String> get() = names.keys

    fun displayName(code: String, locale: Locale = Locale.getDefault()): String {
        val n = names[code.uppercase()] ?: return code.uppercase()
        return when (locale.language) {
            "ru" -> n.ru
            "en" -> n.en
            else -> n.uz
        }
    }

    /**
     * Detects the country of a server from its share-link remark, e.g.
     * "🇩🇪 Frankfurt", "DE-Frankfurt", "Germany Frankfurt" or "Германия".
     * Returns the ISO code (or "") and the remaining text as the city.
     */
    fun detect(remark: String): Pair<String, String> {
        val text = remark.trim()
        flagCode(text)?.let { (code, rest) -> return code to cleanCity(rest) }

        Regex("^([A-Za-z]{2})(?:[\\s_\\-|·:]+(.*))?$").find(text)?.let { m ->
            val code = m.groupValues[1].uppercase()
            if (code in names) return code to cleanCity(m.groupValues[2])
        }
        val lower = text.lowercase()
        for ((code, n) in names) {
            for (name in listOf(n.uz, n.ru, n.en)) {
                val idx = lower.indexOf(name.lowercase())
                if (idx >= 0) {
                    val rest = text.removeRange(idx, idx + name.length)
                    return code to cleanCity(rest)
                }
            }
        }
        return "" to text
    }

    private fun flagCode(text: String): Pair<String, String>? {
        val cps = text.codePoints().toArray()
        if (cps.size < 2) return null
        val base = 0x1F1E6
        val a = cps[0] - base
        val b = cps[1] - base
        if (a !in 0..25 || b !in 0..25) return null
        val code = "${'A' + a}${'A' + b}"
        val rest = String(cps, 2, cps.size - 2)
        return code to rest
    }

    private fun cleanCity(s: String): String =
        s.trim().trim('-', '_', '|', '·', ':', ',').trim()
}
