package io.nekohasekai.sagernet.group

import org.junit.Test
import io.nekohasekai.sagernet.fmt.v2ray.parseV2Ray
import okhttp3.HttpUrl.Companion.toHttpUrl

class ParseTest {
    @Test
    fun testParse() {
        val link = "vless://59ccdf8a-3333-4426-af09-f9442e684ed0@fig.xxee.ru:443?encryption=none&type=ws&security=tls&fp=firefox&path=/28293/fxRNZkfcweb_path&host=fig.xxee.ru#%F0%9F%87%AB%F0%9F%87%AE%20%F0%9F%8E%AE%20%D0%A4%D0%B8%D0%BD%D0%BB%D1%8F%D0%BD%D0%B4%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%B5%D0%B9%D0%BC%D0%B8%D0%BD%D0%B3%D0%B0%20%C2%B7%20WebSocket"
        println("Testing link: $link")
        val bean = parseV2Ray(link)
        println("Result: ${bean.name}")
    }
}
