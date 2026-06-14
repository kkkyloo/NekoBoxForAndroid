# PLAN.md — NekoBox (форк kkkyloo) для РФ

> Рабочий план и «понимание проекта». Собрано из анализа всей истории git, кода и
> тестовых дампов подписки. Язык — русский, потому что так удобнее владельцу форка.
> Сборка идёт **только в GitHub Actions** (локально не собирается), поэтому весь код
> правим «вслепую» и проверяем уже в собранном APK.

---

## 0. TL;DR — что происходит и что делать

Ты:
1. Сделал форк NekoBox, накатил РФ-фиксы (обход блокировок, bypass-правила, язык, UI).
2. Поменял VPN-подписку — в новой появились протоколы, которых у тебя в форке не было.
3. Влил (merge) чужой форк `starifly/main`, где эти протоколы есть (juicity, snell, ssr,
   xhttp, sing-box-extended и т.д.) → словил кучу конфликтов и крашей.
4. Чинил миграции БД и компиляцию → починил запуск.
5. Добавил «Авто-URL» (глобальный urltest по всем профилям), но **не протестировал**.

Сейчас **3 проблемы** и **большой UX/UI рефактор**:

| # | Симптом | Корень (моя гипотеза) | Статус |
|---|---------|------------------------|--------|
| П1 | Base64 (`fmt=v2b64`) и JSON (`fmt=xray-ext`) подписки **не импортируются** | Подписки начинаются со строк-комментариев `#profile-...`, парсер их не отфильтровывал | ✅ Фикс уже в рабочем дереве (`Formats.kt`), не закоммичен |
| П2 | Sing-box (`fmt=sing`) подписка импортируется, но **VPN не работает, пинга нет** | Каждый outbound импортируется как сырой `ConfigBean`; + откат ядра на starifly sing-box | 🔴 Нужна диагностика |
| П3 | UX/UI: лишние настройки, неудобный импорт (окно «выползает»), нет авто-группы и сортировки по пингу | Дизайн «как в оригинале» | 🔴 Не начато |

---

## 1. Что это за проект

**База:** NekoBox for Android (`io.nekohasekai.sagernet`) — Android-клиент VPN/прокси
на ядре **sing-box** (через gomobile/libcore). Поддерживает vmess/vless/trojan/ss/ssr/
hysteria/tuic/wireguard/juicity/snell/anytls и «сырые» конфиги (ConfigBean).

**Твой форк (`kkkyloo`):** заточен под РФ — bypass-правила для российских сервисов,
русский язык, твики UI, авто-URL-выбор сервера, защита от утечек.

**Апстримы:**
- `origin` → `github.com/kkkyloo/NekoBoxForAndroid` (твой)
- `starifly` → локальный `D:\neko\neko2` (форк, откуда влиты новые протоколы и фичи)

**Ключевая особенность сборки:** APK собирается в CI (`.github/workflows/`), ядро
sing-box клонируется и компилируется через gomobile в GitHub Actions. **Локальной
сборки нет** → каждую правку проверяем только после пуша и сборки CI.

---

## 2. История git — как мы сюда пришли

Хронология последних значимых коммитов (новые сверху):

```
c56443e feat: advanced auto url filters and UI tweaks   ← фильтры авто-URL (страна/группа/тест-URL)
214c1e0 fix: manual MIGRATION_7_8 (juicityBean crash)
5d8f0d9 docs: README про Auto URL
8f55c48 fix: финальные ошибки компиляции (userRemarks, ConfigurationFragment)
323a178 fix: ошибки компиляции Kotlin (Auto URL и UI)
c86e04c fix: откат хэша sing-box на starifly      ← ОТКАТ sing-box-extended
16b9cb1 fix: откат URL ядра sing-box (чинит gomobile build)  ← ОТКАТ sing-box-extended
2cea4ad fix: Room migration crash (defaultValue)
dc818cf feat: Auto URL + sing-box-extended       ← ВВЁЛ авто-URL и расширенное ядро
6fb4e37 fix: unpadded base64 в b64Decode          ← фикс парсинга подписок
bc62353 Revert "db migration 8->9"
bacbb4d fix: db migration 8->9 (merge schema mismatch)
7b8417b ci: отключил авто-сборку main
...
abf3330 Merge starifly/main into kkkyloo/main     ← БОЛЬШОЙ MERGE (новые протоколы)
1e4b0b4 bump version (starifly)
```

**Важный вывод по ядру:** в `dc818cf` ты переключил ядро на
`shtorm-7/sing-box-extended`, но оно **сломало gomobile-сборку**, и в `16b9cb1` +
`c86e04c` ты **откатил обратно на `starifly/sing-box`** (commit
`4998428a...`). То есть сейчас приложение собирается на **обычном starifly-ядре**, а не
на «extended». Это напрямую влияет на П2 (см. ниже).

---

## 3. Что merge принёс (новые возможности)

Из `abf3330` (Merge starifly/main) в форк добавлены:
- **Протоколы:** Juicity, Snell, ShadowsocksR, расширенный Shadowsocks, xhttp-транспорт
  (`XhttpExtraConverter.kt`), доработки VLESS/VMess (`StandardV2RayBean`, `V2RayFmt`).
- **БД:** новые схемы `6/7/8.json`, поля в `ProxyEntity`, миграции (источник крашей →
  чинил в `2cea4ad`, `214c1e0`).
- **Бэкап/WebDAV:** `WebDAVSettingsActivity`, переписанный `BackupFragment`.
- **UI:** доработки `ConfigurationFragment`, `GroupSettingsActivity`, `MainActivity`.

Цена: конфликты миграций БД, крэши (`juicityBean`), ошибки компиляции — всё
последовательно зачинено коммитами выше.

---

## 4. Авто-URL — что уже сделано

«Авто-URL» = глобальный режим, при котором приложение строит один `urltest`-outbound из
**всех** профилей и сам выбирает лучший по пингу, минуя обычный selector.

Реализация (коммиты `dc818cf`, `c56443e`):
- **`Key`/`DataStore`:** `globalAutoUrl`, плюс фильтры `autoUrlTestUrl`,
  `autoUrlCountryFilter(+Mode)`, `autoUrlGroupFilter(+Mode)`.
- **`ConfigBuilder.buildConfig`:** если `globalAutoUrl && !forTest` — собирает `urltest`
  по всем профилям (исключая CONFIG/NEKO/CHAIN), с фильтрами по группе и по
  стране/подстроке имени, тест-URL и `tolerance=50`. Файл:
  `app/src/main/java/io/nekohasekai/sagernet/fmt/ConfigBuilder.kt:536`.
- **`BaseService`:** когда `globalAutoUrl` и нет выбранного профиля — подставляет
  виртуальный профиль «🌐 Умный авто-выбор» (id `-999`), чтобы сервис стартовал.
- **`ConfigurationFragment`:** пункт меню `action_global_auto_url` (toggle + reload).
- **`global_preferences.xml` / `arrays.xml`:** UI настроек фильтров.

⚠️ **Не протестировано.** Риски: `urltest` по сотням профилей разом (медленный старт,
расход трафика на проверки), виртуальный профиль `-999` в `BaseService`, взаимодействие
с selector-логикой (`canReloadSelector`).

---

## 5. Проблема П1 — Base64 / JSON подписки не импортируются

**Что показывает дамп.** Файлы `xray.txt` / `v2b64_real.txt` — это base64, и его
расшифровка начинается с метаданных-комментариев:
```
#profile-update-interval: 12
#profile-title: Private VPN
#support-url: https://t.me/prvtvpnsupport
#ping-type: tcp
#announce: base64:...
vless://...
vmess://...
```
Старый `parseProxies` не отбрасывал строки `#...` и пустые → парсинг падал/возвращал
мусор → «не импортируется».

**Фикс (уже в рабочем дереве, не закоммичен)** —
`app/src/main/java/io/nekohasekai/sagernet/ktx/Formats.kt:107`:
```kotlin
val lines = text.split('\n').map { it.trim() }.filter { !it.startsWith("#") && it.isNotBlank() }
val links = lines.flatMap { it.split(' ') }
val linksByLine = lines
```
Это правильное направление. Связка с более ранним фиксом `6fb4e37` (unpadded base64 в
`b64Decode`).

**TODO по П1:**
- [ ] Закоммитить фикс `Formats.kt`.
- [ ] Прогнать `ParseTest.kt` (юнит-тест парсинга) на реальных дампах.
- [ ] Проверить, что `xray-ext` (полный JSON с `outbounds`) тоже заходит: ветка
      `parseJSON` → `json.has("outbounds")` создаёт по `ConfigBean` на каждый outbound
      (`RawUpdater.kt`, ~строка 983). Это пересекается с П2.
- [ ] Убедиться, что `#announce: base64:...` не ломает ничего после фильтрации.

---

## 6. Проблема П2 — Sing-box подписка импортируется, но VPN не работает

**Как импортируется.** `fmt=sing` отдаёт полный sing-box JSON. В `RawUpdater.parseJSON`
ветка `json.has("outbounds")` берёт каждый outbound (кроме dns/block/direct/selector/
urltest) и заворачивает в **`ConfigBean` (type=1, «сырой конфиг»)** — по профилю на
outbound. Имена/типы из дампа `sing.txt`: `vless TCP (flow=xtls-rprx-vision)`,
`vless WS`, `trojan gRPC` и т.д.

**Почему может не быть пинга — гипотезы (по убыванию вероятности):**

1. **Откат ядра.** Подписка/конфиг рассчитаны на `sing-box-extended` (shtorm-7), а
   собирается **starifly sing-box** (откат в `16b9cb1`/`c86e04c`). Если outbound
   использует поля/протокол, которых нет в starifly-ядре → конфиг невалиден, туннель не
   поднимается. **Проверить в первую очередь** (лог ядра при старте).
2. **Сырой outbound без контекста.** `ConfigBean` хранит **один** outbound, а NekoBox
   потом сам достраивает route/dns/inbounds вокруг него. Если в outbound остались
   ссылки на теги/transport/tls из исходного полного конфига (detour, dns-теги), они
   «повисают» → ошибка сборки или нет хэндшейка.
3. **TLS/Reality/flow.** `xtls-rprx-vision` + reality требуют корректного маппинга в
   sing-box. При переносе как сырой JSON флаги могли потеряться/исказиться.
4. **DNS/route утечки или блокировка.** РФ-bypass-правила (`createRule`, commit
   `775116b`/`b8ac2f9`) могут конфликтовать с маршрутизацией нового конфига.

**TODO по П2 (диагностика):**
- [ ] Снять **лог ядра** при подключении sing-box профиля (Logs / logcat в CI-сборке).
- [ ] Глянуть итоговый конфиг, который строит `ConfigBuilder` для одного `ConfigBean`
      (есть ли висячие теги/невалидные поля).
- [ ] Решить вопрос ядра: либо **починить сборку sing-box-extended** (исходная цель),
      либо убедиться, что starifly-ядро поддерживает все протоколы подписки.
- [ ] Сверить версию схемы sing-box конфига (`sing.txt`) с версией ядра.
- [ ] Минимальный тест: импортировать **один** vless-TCP сервер и проверить пинг
      изолированно (без авто-URL, без bypass-правил).

> ⚠️ Гипотезы. Точную причину покажет лог ядра — без него гадаем.

---

## 7. UX/UI рефактор — что хочет владелец

Требования из текста, разложенные по пунктам:

### 7.1. Убрать лишние настройки
- Спрятать «продвинутые» настройки в раздел **«Подробные/Расширенные»**.
- На главном экране оставить только нужное обычному пользователю:
  - **Выбор приложений для VPN** (split-tunneling: только выбранные апки идут через VPN).
  - **Настройка Авто-URL**.
- Сейчас всё свалено в `global_preferences.xml` (General / Route / Fragment / DNS / …) —
  нужно реструктурировать: «простой» экран + «расширенный» подэкран.

### 7.2. Удобный импорт
- Не должно «выползать окно» — импорт **автоматически**, без модалок.
- Вставил ссылку → само импортировалось.
- Точка входа импорта — `ConfigurationFragment` (обработка clipboard/deep-link/QR).

### 7.3. Авто-группа для Авто-URL
- При включении Авто-URL **автоматически создавать отдельную группу**.
- Сортировка/отображение так, чтобы было видно: **что используется в авто** (сверху), а
  **ниже — список всех групп**, между которыми можно переключаться.
- Все VPN-профили туда; **сортировка по пингу**.

### 7.4. Переработать интерфейс
- Кнопки — **внизу** экрана (bottom bar), не сбоку.
- Убрать всё лишнее/отвлекающее.

### 7.5. Референс
- `E:\podcop\podkop-plus` — посмотреть, **как сделан Авто-URL** там.
  ⚠️ Это пакет для **OpenWrt** (LuaCI/shell), архитектура совсем другая, чем Android-app.
  Берём оттуда **идею/логику** авто-выбора, не код.

**TODO по UX (черновой порядок):**
- [ ] Аудит `global_preferences.xml`: пометить «простые» vs «расширенные».
- [ ] Сделать подэкран «Расширенные настройки».
- [ ] Вынести «Выбор приложений» и «Авто-URL» на видное место.
- [ ] Авто-импорт без модального окна.
- [ ] Авто-создание группы при включении Авто-URL + сортировка групп.
- [ ] Сортировка профилей по пингу.
- [ ] Перенести кнопки вниз (bottom bar; уже есть `SHOW_BOTTOM_BAR`/`showBottomBar`).
- [ ] Изучить логику авто-выбора в `E:\podcop\podkop-plus`.

---

## 8. Гигиена репозитория (сделать сразу)

В корень репозитория **закоммичены отладочные дампы** (`dc818cf`):
```
clash.txt  clash_real.txt  log.txt  sing.txt  sing_real.txt  v2b64_real.txt  xray.txt
```
🔴 **Это утечка:** в них реальные данные подписки (server, uuid `59ccdf8a-...`,
пароли trojan, support-URL). Нужно:
- [ ] **Удалить файлы** из репо и добавить в `.gitignore`.
- [ ] Так как они уже в истории — по-хорошему **переписать историю** (BFG / filter-repo)
      и **сменить ключи** подписки у провайдера (данные уже «утекли» в git).
- [ ] Удалить `log.txt`.

---

## 9. Приоритеты (предлагаемый порядок)

1. **Гигиена + П1:** удалить дампы из репо, закоммитить фикс `Formats.kt`, прогнать
   `ParseTest`, собрать CI, проверить импорт base64/json. *(быстро, разблокирует тест)*
2. **П2 диагностика:** снять лог ядра sing-box, понять корень (ядро vs конфиг),
   определиться с sing-box-extended. *(критично для рабочего VPN)*
3. **Авто-URL:** протестировать существующую реализацию, потом авто-группа + сортировка
   по пингу.
4. **UX/UI:** реструктуризация настроек, авто-импорт, bottom bar.

---

## 10. Ключевые файлы (карта)

| Область | Файл |
|--------|------|
| Парсинг ссылок/base64 | `app/.../ktx/Formats.kt` (`parseProxies`, `b64Decode`) |
| Импорт подписок/форматов | `app/.../group/RawUpdater.kt` (`parseRaw`, `parseJSON`) |
| Сборка конфига sing-box + Авто-URL | `app/.../fmt/ConfigBuilder.kt:536` |
| Старт/reload сервиса + Авто-URL | `app/.../bg/BaseService.kt` |
| Главный экран профилей/групп/импорт/меню | `app/.../ui/ConfigurationFragment.kt` |
| Ключи настроек | `app/.../Constants.kt` (`object Key`) |
| Значения настроек | `app/.../database/DataStore.kt` |
| UI настроек | `app/src/main/res/xml/global_preferences.xml`, `res/values/arrays.xml` |
| Ядро sing-box (версия/URL) | `buildScript/lib/core/get_source.sh`, `get_source_env.sh` |
| Тест парсинга | `app/src/test/.../group/ParseTest.kt` |
| CI-сборка | `.github/workflows/build.yml`, `ci.yml` |

---

*Обновляй этот файл по мере продвижения. Каждую правку проверяем через CI-сборку —
локально не собирается.*
