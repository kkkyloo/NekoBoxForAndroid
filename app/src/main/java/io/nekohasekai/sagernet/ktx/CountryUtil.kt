package io.nekohasekai.sagernet.ktx

/**
 * Detects the country of a proxy server from its display name, primarily by decoding the
 * flag emoji (regional indicator symbols, e.g. 🇺🇸 → "US"), with a text-name fallback.
 *
 * Ported from podkop's `country_from_flag_emoji` (sing_box_runtime.uc): two consecutive
 * Unicode regional indicator code points (U+1F1E6..U+1F1FF) map to an ISO 3166-1 alpha-2
 * country code. Used by the Auto-URL country filter and its picker.
 */
object CountryUtil {

    data class Country(val code: String, val flag: String, val name: String)

    // ISO code -> flag emoji + Russian display name. Common VPN exit countries.
    val KNOWN: LinkedHashMap<String, Country> = linkedMapOf(
        "RU" to Country("RU", "🇷🇺", "Россия"),
        "UA" to Country("UA", "🇺🇦", "Украина"),
        "BY" to Country("BY", "🇧🇾", "Беларусь"),
        "KZ" to Country("KZ", "🇰🇿", "Казахстан"),
        "US" to Country("US", "🇺🇸", "США"),
        "GB" to Country("GB", "🇬🇧", "Великобритания"),
        "DE" to Country("DE", "🇩🇪", "Германия"),
        "NL" to Country("NL", "🇳🇱", "Нидерланды"),
        "FR" to Country("FR", "🇫🇷", "Франция"),
        "FI" to Country("FI", "🇫🇮", "Финляндия"),
        "SE" to Country("SE", "🇸🇪", "Швеция"),
        "NO" to Country("NO", "🇳🇴", "Норвегия"),
        "PL" to Country("PL", "🇵🇱", "Польша"),
        "LV" to Country("LV", "🇱🇻", "Латвия"),
        "LT" to Country("LT", "🇱🇹", "Литва"),
        "EE" to Country("EE", "🇪🇪", "Эстония"),
        "CH" to Country("CH", "🇨🇭", "Швейцария"),
        "AT" to Country("AT", "🇦🇹", "Австрия"),
        "IT" to Country("IT", "🇮🇹", "Италия"),
        "ES" to Country("ES", "🇪🇸", "Испания"),
        "TR" to Country("TR", "🇹🇷", "Турция"),
        "AE" to Country("AE", "🇦🇪", "ОАЭ"),
        "JP" to Country("JP", "🇯🇵", "Япония"),
        "SG" to Country("SG", "🇸🇬", "Сингапур"),
        "HK" to Country("HK", "🇭🇰", "Гонконг"),
        "KR" to Country("KR", "🇰🇷", "Корея"),
        "IN" to Country("IN", "🇮🇳", "Индия"),
        "CA" to Country("CA", "🇨🇦", "Канада"),
        "BR" to Country("BR", "🇧🇷", "Бразилия"),
        "AU" to Country("AU", "🇦🇺", "Австралия"),
        "CN" to Country("CN", "🇨🇳", "Китай"),
        "TW" to Country("TW", "🇹🇼", "Тайвань"),
        "AM" to Country("AM", "🇦🇲", "Армения"),
        "GE" to Country("GE", "🇬🇪", "Грузия"),
        "AZ" to Country("AZ", "🇦🇿", "Азербайджан"),
        "MD" to Country("MD", "🇲🇩", "Молдова"),
        "RO" to Country("RO", "🇷🇴", "Румыния"),
        "CZ" to Country("CZ", "🇨🇿", "Чехия"),
        "IE" to Country("IE", "🇮🇪", "Ирландия"),
        "IL" to Country("IL", "🇮🇱", "Израиль"),
    )

    // Text fallback: lowercase substrings (ru/en) -> ISO code, for names without a flag emoji.
    private val TEXT_HINTS: Map<String, String> = linkedMapOf(
        "росси" to "RU", "russia" to "RU", "moscow" to "RU", "москв" to "RU",
        "украин" to "UA", "ukraine" to "UA",
        "беларус" to "BY", "belarus" to "BY",
        "казахстан" to "KZ", "kazakh" to "KZ",
        "сша" to "US", "united states" to "US", "usa" to "US", "america" to "US",
        "британ" to "GB", "england" to "GB", "london" to "GB", "kingdom" to "GB",
        "герман" to "DE", "germany" to "DE", "frankfurt" to "DE",
        "нидерланд" to "NL", "netherland" to "NL", "amsterdam" to "NL", "holland" to "NL",
        "франц" to "FR", "france" to "FR", "paris" to "FR",
        "финлянд" to "FI", "finland" to "FI", "helsinki" to "FI",
        "швеци" to "SE", "sweden" to "SE", "stockholm" to "SE",
        "норвег" to "NO", "norway" to "NO",
        "польш" to "PL", "poland" to "PL", "warsaw" to "PL",
        "латви" to "LV", "latvia" to "LV",
        "литв" to "LT", "lithuania" to "LT",
        "эстони" to "EE", "estonia" to "EE",
        "швейцар" to "CH", "switzerland" to "CH", "zurich" to "CH",
        "австри" to "AT", "austria" to "AT", "vienna" to "AT",
        "итали" to "IT", "italy" to "IT", "milan" to "IT",
        "испани" to "ES", "spain" to "ES", "madrid" to "ES",
        "турц" to "TR", "turkey" to "TR", "istanbul" to "TR",
        "эмират" to "AE", "dubai" to "AE", "emirates" to "AE",
        "япони" to "JP", "japan" to "JP", "tokyo" to "JP",
        "сингапур" to "SG", "singapore" to "SG",
        "гонконг" to "HK", "hong kong" to "HK", "hongkong" to "HK",
        "коре" to "KR", "korea" to "KR", "seoul" to "KR",
        "инди" to "IN", "india" to "IN", "mumbai" to "IN",
        "канад" to "CA", "canada" to "CA",
        "бразили" to "BR", "brazil" to "BR",
        "австрали" to "AU", "australia" to "AU", "sydney" to "AU",
        "китай" to "CN", "china" to "CN",
        "тайван" to "TW", "taiwan" to "TW",
        "армен" to "AM", "armenia" to "AM",
        "грузи" to "GE", "georgia" to "GE",
        "азербайджан" to "AZ", "azerbaijan" to "AZ",
        "молдов" to "MD", "moldova" to "MD",
        "румын" to "RO", "romania" to "RO",
        "чехи" to "CZ", "czech" to "CZ", "prague" to "CZ",
        "ирланд" to "IE", "ireland" to "IE", "dublin" to "IE",
        "израил" to "IL", "israel" to "IL",
    )

    /** Decode the first flag emoji in [name] to an ISO 3166-1 alpha-2 code, or null. */
    fun flagToCode(name: String): String? {
        var i = 0
        var prev = -1
        while (i < name.length) {
            val cp = name.codePointAt(i)
            if (cp in 0x1F1E6..0x1F1FF) {
                if (prev in 0x1F1E6..0x1F1FF) {
                    val c1 = 'A' + (prev - 0x1F1E6)
                    val c2 = 'A' + (cp - 0x1F1E6)
                    return "$c1$c2"
                }
                prev = cp
            } else {
                prev = -1
            }
            i += Character.charCount(cp)
        }
        return null
    }

    /** Best-effort country code for a server name: flag emoji first, then text hints. */
    fun codeOf(name: String): String? {
        flagToCode(name)?.let { return it }
        val lower = name.lowercase()
        for ((hint, code) in TEXT_HINTS) {
            if (lower.contains(hint)) return code
        }
        return null
    }

    /** Human label for a code: "🇺🇸 США" if known, else the bare code. */
    fun displayOf(code: String): String {
        val c = KNOWN[code.uppercase()]
        return if (c != null) "${c.flag} ${c.name}" else code.uppercase()
    }
}
