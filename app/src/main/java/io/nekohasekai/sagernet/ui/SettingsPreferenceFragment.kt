package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.preference.EditTextPreferenceModifiers
import io.nekohasekai.sagernet.ktx.*
import io.nekohasekai.sagernet.utils.Theme
import moe.matsuri.nb4a.ui.*

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    private lateinit var isProxyApps: SwitchPreference

    private lateinit var globalCustomConfig: EditConfigPreference


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.layoutManager = FixedLinearLayoutManager(listView)
    }

    private val reloadListener = Preference.OnPreferenceChangeListener { _, _ ->
        needReload()
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DataStore.configurationStore
        DataStore.initGlobal()
        addPreferencesFromResource(R.xml.global_preferences)

        val appTheme = findPreference<ColorPickerPreference>(Key.APP_THEME)!!
        appTheme.setOnPreferenceChangeListener { _, newTheme ->
            if (DataStore.serviceState.started) {
                SagerNet.reloadService()
            }
            val theme = Theme.getTheme(newTheme as Int)
            app.setTheme(theme)
            requireActivity().apply {
                setTheme(theme)
                ActivityCompat.recreate(this)
            }
            true
        }

        val nightTheme = findPreference<SimpleMenuPreference>(Key.NIGHT_THEME)!!
        nightTheme.setOnPreferenceChangeListener { _, newTheme ->
            Theme.currentNightMode = (newTheme as String).toInt()
            Theme.applyNightTheme()
            true
        }
        val mixedPort = findPreference<EditTextPreference>(Key.MIXED_PORT)!!
        val enableLocalProxyInVpn = findPreference<SwitchPreference>(Key.ENABLE_LOCAL_PROXY_IN_VPN)!! 
        val mixedUsername = findPreference<EditTextPreference>(Key.MIXED_USERNAME)!!
        val mixedPassword = findPreference<EditTextPreference>(Key.MIXED_PASSWORD)!!
        val serviceMode = findPreference<Preference>(Key.SERVICE_MODE)!!
        val allowAccess = findPreference<SwitchPreference>(Key.ALLOW_ACCESS)!!
        val appendHttpProxy = findPreference<SwitchPreference>(Key.APPEND_HTTP_PROXY)!!
        appendHttpProxy.isEnabled = DataStore.enableLocalProxyInVpn 
        enableLocalProxyInVpn.setOnPreferenceChangeListener { _, newValue ->
            appendHttpProxy.isEnabled = newValue as Boolean
            reloadListener.onPreferenceChange(enableLocalProxyInVpn, newValue)
        }

        val showDirectSpeed = findPreference<SwitchPreference>(Key.SHOW_DIRECT_SPEED)!!
        val ipv6Mode = findPreference<Preference>(Key.IPV6_MODE)!!
        val trafficSniffing = findPreference<Preference>(Key.TRAFFIC_SNIFFING)!!

        val bypassLan = findPreference<SwitchPreference>(Key.BYPASS_LAN)!!
        val bypassLanInCore = findPreference<SwitchPreference>(Key.BYPASS_LAN_IN_CORE)!!

        val remoteDns = findPreference<EditTextPreference>(Key.REMOTE_DNS)!!
        val directDns = findPreference<EditTextPreference>(Key.DIRECT_DNS)!!
        val enableDnsRouting = findPreference<SwitchPreference>(Key.ENABLE_DNS_ROUTING)!!
        val enableFakeDns = findPreference<SwitchPreference>(Key.ENABLE_FAKEDNS)!!

        val logLevel = findPreference<LongClickListPreference>(Key.LOG_LEVEL)!!
        val mtu = findPreference<MTUPreference>(Key.MTU)!!
        globalCustomConfig = findPreference(Key.GLOBAL_CUSTOM_CONFIG)!!
        globalCustomConfig.useConfigStore(Key.GLOBAL_CUSTOM_CONFIG)

        logLevel.dialogLayoutResource = R.layout.layout_loglevel_help
        logLevel.setOnPreferenceChangeListener { _, _ ->
            needRestart()
            true
        }
        logLevel.setOnLongClickListener {
            if (context == null) return@setOnLongClickListener true

            val view = EditText(context).apply {
                inputType = EditorInfo.TYPE_CLASS_NUMBER
                var size = DataStore.logBufSize
                if (size == 0) size = 50
                setText(size.toString())
            }

            MaterialAlertDialogBuilder(requireContext()).setTitle("Log buffer size (kb)")
                .setView(view)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    DataStore.logBufSize = view.text.toString().toInt()
                    if (DataStore.logBufSize <= 0) DataStore.logBufSize = 50
                    needRestart()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }

        mixedPort.setOnBindEditTextListener(EditTextPreferenceModifiers.Port)

        val metedNetwork = findPreference<Preference>(Key.METERED_NETWORK)!!
        if (Build.VERSION.SDK_INT < 28) {
            metedNetwork.remove()
        }
        isProxyApps = findPreference(Key.PROXY_APPS)!!
        isProxyApps.setOnPreferenceChangeListener { _, newValue ->
            startActivity(Intent(activity, AppManagerActivity::class.java))
            if (newValue as Boolean) DataStore.dirty = true
            newValue
        }

        val profileTrafficStatistics =
            findPreference<SwitchPreference>(Key.PROFILE_TRAFFIC_STATISTICS)!!
        val speedInterval = findPreference<SimpleMenuPreference>(Key.SPEED_INTERVAL)!!
        profileTrafficStatistics.isEnabled = speedInterval.value.toString() != "0"
        speedInterval.setOnPreferenceChangeListener { _, newValue ->
            profileTrafficStatistics.isEnabled = newValue.toString() != "0"
            needReload()
            true
        }

        serviceMode.setOnPreferenceChangeListener { _, _ ->
            if (DataStore.serviceState.started) SagerNet.stopService()
            true
        }

        val tunImplementation = findPreference<SimpleMenuPreference>(Key.TUN_IMPLEMENTATION)!!
        val resolveDestination = findPreference<SwitchPreference>(Key.RESOLVE_DESTINATION)!!
        val acquireWakeLock = findPreference<SwitchPreference>(Key.ACQUIRE_WAKE_LOCK)!!
        val enableClashAPI = findPreference<SwitchPreference>(Key.ENABLE_CLASH_API)!!
        enableClashAPI.setOnPreferenceChangeListener { _, newValue ->
            (activity as MainActivity?)?.refreshNavMenu(newValue as Boolean)
            needReload()
            true
        }

        mixedPort.onPreferenceChangeListener = reloadListener
        mixedUsername.onPreferenceChangeListener = reloadListener
        mixedPassword.onPreferenceChangeListener = reloadListener
        appendHttpProxy.onPreferenceChangeListener = reloadListener
        showDirectSpeed.onPreferenceChangeListener = reloadListener
        trafficSniffing.onPreferenceChangeListener = reloadListener
        bypassLan.onPreferenceChangeListener = reloadListener
        bypassLanInCore.onPreferenceChangeListener = reloadListener
        mtu.onPreferenceChangeListener = reloadListener

        enableFakeDns.onPreferenceChangeListener = reloadListener
        remoteDns.onPreferenceChangeListener = reloadListener
        directDns.onPreferenceChangeListener = reloadListener
        enableDnsRouting.onPreferenceChangeListener = reloadListener

        ipv6Mode.onPreferenceChangeListener = reloadListener

        // === ПРЕДУПРЕЖДЕНИЯ ДЛЯ ЛОКАЛЬНОГО ПОРТА ===
        allowAccess.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⚠️ Угроза деанонимизации!")
                    .setMessage("Открытие локального порта позволит раздавать VPN по Wi-Fi, но создаст обходной путь для шпионских приложений.\n\nОни смогут подключиться к локальному порту и узнать ваш VPN IP, минуя защиту TUN-интерфейса.\n\nВключайте только для раздачи VPN!")
                    .setPositiveButton("Включить (Опасно)") { _, _ ->
                        allowAccess.isChecked = true
                        reloadListener.onPreferenceChange(allowAccess, true)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                false // Не переключать свитч сразу, ждем подтверждения
            } else {
                reloadListener.onPreferenceChange(allowAccess, false)
                true // Разрешаем выключить мгновенно
            }
        }

        enableLocalProxyInVpn.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⚠️ Риск утечки IP!")
                    .setMessage("Внедрение HTTP-прокси в систему требует открытия локального порта (как и настройка выше). Это позволяет вредоносам обойти изоляцию туннеля.\n\nВключайте только если у вас проблемы с загрузкой некоторых сайтов в браузере.")
                    .setPositiveButton("Включить (Опасно)") { _, _ ->
                        enableLocalProxyInVpn.isChecked = true
                        appendHttpProxy.isEnabled = true
                        reloadListener.onPreferenceChange(enableLocalProxyInVpn, true)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                false
            } else {
                appendHttpProxy.isEnabled = false
                reloadListener.onPreferenceChange(enableLocalProxyInVpn, false)
                true
            }
        }
        // ===========================================


        resolveDestination.onPreferenceChangeListener = reloadListener
        tunImplementation.onPreferenceChangeListener = reloadListener
        acquireWakeLock.onPreferenceChangeListener = reloadListener
        globalCustomConfig.onPreferenceChangeListener = reloadListener

        // ── VPN Watchdog test button ─────────────────────────────────
        findPreference<Preference>("vpnWatchdogTest")?.setOnPreferenceClickListener {
    if (!DataStore.serviceState.connected) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Тест watchdog")
            .setMessage("VPN не запущен. Сначала подключитесь к VPN.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
        return@setOnPreferenceClickListener true
    }

    // Блок повторного нажатия пока тест идёт
    if (io.nekohasekai.sagernet.bg.VpnWatchdog.testModeRequested) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⏳ Тест уже выполняется")
            .setMessage("Подожди завершения текущего теста.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
        return@setOnPreferenceClickListener true
    }

    val intervalSec = DataStore.vpnWatchdogInterval.coerceAtLeast(3)
    val estSec = intervalSec * 2 + 3

    MaterialAlertDialogBuilder(requireContext())
        .setTitle("🧪 Тест авто-переподключения")
        .setMessage(
            "Watchdog симулирует обрыв соединения.\n\n" +
            "• VPN-иконка останется — сервис не перезапускается\n" +
            "• Туннель восстановится примерно через ~${estSec} сек\n" +
            "• Браузер или игра переподключатся сами\n\n" +
            "Открой браузер заранее чтобы увидеть восстановление."
        )
        .setPositiveButton("▶ Запустить") { _, _ ->
            io.nekohasekai.sagernet.bg.VpnWatchdog.testModeRequested = true
            com.google.android.material.snackbar.Snackbar
                .make(
                    requireView(),
                    "🧪 Тест запущен — ждём ~${estSec} сек...",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                )
                .show()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    true
}

        val vpnWatchdogInterval = findPreference<EditTextPreference>(Key.VPN_WATCHDOG_INTERVAL)
        vpnWatchdogInterval?.setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        vpnWatchdogInterval?.onPreferenceChangeListener = reloadListener
        // ────────────────────────────────────────────────────────────
    }

    override fun onResume() {
        super.onResume()

        if (::isProxyApps.isInitialized) {
            isProxyApps.isChecked = DataStore.proxyApps
        }
        if (::globalCustomConfig.isInitialized) {
            globalCustomConfig.notifyChanged()
        }
    }

}