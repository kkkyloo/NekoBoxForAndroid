package io.nekohasekai.sagernet.bg

import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.Logs
import kotlinx.coroutines.*
import libcore.Libcore
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build

class VpnWatchdog(private val service: BaseService.Interface) {

    companion object {
        private const val FAIL_THRESHOLD  = 2
        private const val HTTP_TIMEOUT_MS = 3_000

        @Volatile
        var testModeRequested = false
    }

    // ФИX 1: Свой скоуп вместо GlobalScope — не будет зомби-горутин
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var consecutiveFailures = 0
    private var lastRestartAt = 0L

    fun start() {
        if (!DataStore.vpnWatchdogEnabled) return
        job?.cancel()
        consecutiveFailures = 0
        testModeRequested = false  // ФИX 3: Сброс флага на старте

        job = scope.launch {
            Logs.d("VpnWatchdog: запущен")
            delay(10_000L)

            while (isActive) {
                val intervalSec = DataStore.vpnWatchdogInterval.coerceAtLeast(3)
                val checkIntervalMs = intervalSec * 1000L
                val minRestartMs = checkIntervalMs * 3

                runCatching { check(minRestartMs) }
                    .onFailure { Logs.w("VpnWatchdog error", it) }

                delay(checkIntervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        consecutiveFailures = 0
        testModeRequested = false  // ФИX 3: Сброс флага при остановке
        Logs.d("VpnWatchdog: остановлен")
        // ВАЖНО: scope НЕ отменяем — он переиспользуется при следующем start()
    }

    private suspend fun check(minRestartIntervalMs: Long) {
        if (service.data.state != BaseService.State.Connected) {
            consecutiveFailures = 0
            return
        }

        val url = DataStore.connectionTestURL
        val box = service.data.proxy?.box ?: return

        // Тест-режим: усыпляем ядро при первом чеке
        if (testModeRequested && consecutiveFailures == 0) {
            Logs.w("Watchdog: 🧪 ТЕСТ - Усыпляю ядро")
            box.sleep()
            showToast("🧪 ТЕСТ: Интернет отключен! Ждем обнаружения...")
        }

        val reachable = try {
            Libcore.urlTest(box, url, HTTP_TIMEOUT_MS) > 0
        } catch (e: Exception) {
            false
        }

        if (reachable) {
            if (consecutiveFailures > 0) {
                Logs.d("Watchdog: связь восстановлена сама")
            }
            consecutiveFailures = 0
            return
        }

        consecutiveFailures++
        Logs.w("Watchdog: обнаружен лаг (#$consecutiveFailures)")
        showToast("Watchdog: лаг сети ($consecutiveFailures/$FAIL_THRESHOLD)")

        if (consecutiveFailures >= FAIL_THRESHOLD) {
    val now = System.currentTimeMillis()

    if (!testModeRequested && (now - lastRestartAt < minRestartIntervalMs)) {
        Logs.d("Watchdog: слишком рано, ждём")
        return
    }

    lastRestartAt = now
    consecutiveFailures = 0

    Logs.w("Watchdog: ▶ МЯГКОЕ ВОССТАНОВЛЕНИЕ (без рестарта VPN)")
    showToast("⚠️ Переподключение туннеля...")

    try {
        // Если тест — ядро уже спит, будим его
        // Если реальный обрыв — циклируем ядро
        if (!testModeRequested) {
            box.sleep()
            delay(800L) // даём ядру корректно уснуть
        }

        testModeRequested = false

        box.wake() // пробуждаем — sing-box восстанавливает исходящие соединения
        delay(300L)

        // Сбрасываем клиентские сокеты — Brawl Stars переподключится к серверу
        Libcore.resetAllConnections(true)

        Logs.w("Watchdog: ✅ туннель восстановлен")
        showToast("✅ Туннель восстановлен")
    } catch (e: Exception) {
        Logs.w("Watchdog: ошибка восстановления", e)
        testModeRequested = false
    }
}
    }

    private suspend fun showToast(msg: String) {
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(
                SagerNet.application, msg, android.widget.Toast.LENGTH_SHORT
            ).show()
            try {
                val v = SagerNet.application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(150)
                }
            } catch (_: Exception) {}
        }
    }
}