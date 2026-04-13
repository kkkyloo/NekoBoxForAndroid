# NekoBox RU Fork

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Releases](https://img.shields.io/github/v/release/kkkyloo/NekoBoxForAndroid)](https://github.com/kkkyloo/NekoBoxForAndroid/releases)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

Форк NekoBox For Android (база 1.4.2) c фиксами утечек трафика/DNS и ру правилами для маршрутов

## 🚀 Быстрый старт

1. **Обновите базы:** *Меню -> Маршруты -> Три точки -> Управление ресурсами*. Нажмите на иконку загрузки у файлов `geoip.db` и `geosite.db`. *(Приложение само напомнит об этом при первом запуске).*
2. **Настройте режим VPN:** *Настройки -> Режим VPN для приложений -> Включить -> Выбрать "Прокси"*. Отметьте только те приложения, которым нужен обход блокировок (рекомендуемый и самый безопасный режим).

## 📥 Скачать
[GitHub Releases](https://github.com/kkkyloo/NekoBoxForAndroid/releases)

## 🛡 Ключевые изменения безопасности (RKN Hardening)

### 1. Строгая маршрутизация и изоляция туннеля
В оригинальных клиентах приложения, добавленные в исключения (Bypass), могли направлять трафик через туннель и выявлять IP VPN-сервера.
* В ядре (sing-box) принудительно включена функция `find_process`.
* Трафик исключенных приложений строго фильтруется по UID, предотвращая любые случайные утечки IP.
* *Тест: команда `curl 2ip.io --interface tun0` в Termux завершится ошибкой, если приложению не выдан доступ к VPN.*

### 2. Безопасность локального SOCKS5/HTTP прокси
* По умолчанию отключен локальный порт 10808 (закрытие потенциальной уязвимости обхода TUN-интерфейса).
* При включении опции «Разрешить подключения из локальной сети» на прокси-порт автоматически генерируется уникальный криптографический пароль. (сейчас вырезал включение lan, потому что были проблемы в RKN Hardening. пока что нет времени разбираться)

### 3. Защита от подмены IP 
* sniff_override_destination и sniff включены по умолчанию. Подробнее - https://4pda.to/forum/index.php?showtopic=1120825&view=findpost&p=142912913

---

## 🇷🇺 Адаптация для РФ и стабильность

### 1. Bypass из коробки
* Предустановлены правила для прямого доступа (Direct) к зонам `.ru`, `.рф`, `.su`
* Диапазон российских IP-адресов (`geoip:ru`) автоматически пускается в обход VPN.
* Тест подключения (Ping) переведен на протокол HTTPS

---

## ⚙️ Улучшения интерфейса (UX)

При попытке запустить VPN без баз `geosite`/`geoip` приложение не упадет (краш ядра), а покажет окно с предложением их скачать.

---

## Credits

Оригинальный проект: [MatsuriDayo/NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid)

Core:
- [SagerNet/sing-box](https://github.com/SagerNet/sing-box)

Android GUI:
-[shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
-[SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)
