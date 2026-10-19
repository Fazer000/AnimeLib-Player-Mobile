# AnimeLib Video Player

![Platform](https://img.shields.io/badge/Platform-Android-78C257?style=flat-square)
![Language](https://img.shields.io/badge/Language-Java-ED8B00?style=flat-square)
![UI](https://img.shields.io/badge/UI-Android%20Views%20%2F%20XML%20%2F%20M3-007ACC?style=flat-square)
![Player](https://img.shields.io/badge/Player-AndroidX%20Media3%20ExoPlayer-FF7A00?style=flat-square)

Современный и функциональный видеоплеер для Android, созданный на базе кастомного плеера оригинального проекта [AnimeLib-Mobile](https://github.com/Fazer000/AnimeLib-Mobile). Приложение сочетает в себе удобный проводник медиафайлов, менеджер плейлистов и продвинутый нативный видеоплеер с широкими возможностями кастомизации.

---

## 📌 Авторство и правовая информация

> **Важное примечание:**
> Приложение **AnimeLib** является сторонним клиентом и видеоплеером. Все права на контент, бренд, API, веб-сервисы и материалы принадлежат официальному ресурсу [animelib.org](https://animelib.org) и компаниям-правообладателям (**ООО «Мангалиб»**).
> 
> Приложение не распространяет защищённый авторским правом контент напрямую.
> 
> **Основа кастомного плеера:**
> Данное приложение использует архитектуру и наработки кастомного видеоплеера из открытого проекта [AnimeLib-Mobile](https://github.com/Fazer000/AnimeLib-Mobile) от **Fazer000**.
> 
> Оригинальный плеер был расширен и адаптирован для работы с локальным медиахранилищем, кастомными плейлистами, гибким проводником видеофайлов и улучшенным портретным интерфейсом.

---

## 📱 О приложении

**AnimeLib Video Player** разработан для комфортного просмотра аниме-эпизодов и видеозаписей на мобильных устройствах Android. Приложение автоматически сканирует память устройства, группирует файлы по папкам, генерирует качественные превью и сохраняет прогресс просмотра.

---

## ✨ Основные возможности

* 📁 **Встроенный проводник и файловый менеджер**:
  * **Умная сортировка**: Сортировка медиафайлов по названию (А-Я / Я-А), дате изменения (сначала новые / старые) и размеру файла.
  * **Интерактивная карусель папок**: Быстрый фильтр видео по директориям.
  * **Умные превью**: Генерация уникальных ключевых кадров без повторяющихся заставок и пустых калибров.
  * **Быстрый доступ**: Плавающая карточка последнего просмотренного видео с точным прогресс-баром M3.

* 🎬 **Продвинутый видеоплеер (AnimeLib Player)**:
  * **Адаптивная фоновая подсветка (Ambient Light)**: Динамическое размытие и свечение фона под цвет воспроизводимого кадра.
  * **Объёмный 3D-звук (Surround Sound)**: Кастомная аудио-обработка для глубокого звучания.
  * **Видеофильтры и цветокоррекция**: Точная настройка яркости, контраста, насыщенности, гаммы и оттенка.
  * **Удобный портретный режим**:
    * Эргономичная верхняя карточка с метаданными и форматом файла (1080p, MKV, MP4).
    * Быстрый доступ к добавлению в плейлист, выбору скорости (`1.0x`–`2.0x`), настройкам звука, субтитров и деталям.
    * Интерактивный список серий/файлов с отслеживанием прогресса.
  * **Жесты и управление**:
    * Свайпы для регулировки громкости и яркости.
    * Двойной тап для настраиваемой перемотки (пропуск опенингов/эндингов).
    * Удержание для ускорения воспроизведения (2x).
  * **Режим «Картинка в картинке» (PiP)**: Фоновый просмотр поверх других приложений.

* 🎶 **Управление плейлистами**:
  * Создание пользовательских плейлистов в формате M3 BottomSheet.
  * Переключение и проверка наличия видео в плейлистах с помощью удобных галочек и настроек.
  * Управление порядком и быстрый переход между эпизодами.

* 🎨 **Material Design 3 и персонализация**:
  * Полная поддержка тёмной и светлой темы оформления.
  * Плавные анимации и современные компоненты интерфейса.

---

## 🛠 Технические характеристики

### Требования
* **Минимальная версия Android**: 10.0 (API Level 29)
* **Целевая версия Android**: 15.0 (API Level 35)
* **Язык разработки**: Java 11 / Kotlin
* **Архитектурный подход**: MVVM + Android Architecture Components

### Используемый стек
* **UI & Layouts**: Material Design 3, ViewBinding, Custom Views, BottomSheetBehavior
* **Media Player Engine**: AndroidX Media3 (ExoPlayer, UI, OkHttp Datasource)
* **Database & Persistence**: Room Database (Entities, DAOs, DatabaseManager)
* **Thumbnail Generation**: Android MediaMetadataRetriever + MediaStore

---

## 📥 Скачать

| Платформа / Версия | Последний релиз | Ссылка |
|-------------------|-----------------|--------|
| 🎬 **AnimeLib Video Player (Android)** | [![Player Release](https://img.shields.io/badge/release-v1.0.0-FF7A00?style=flat-square)](#) | [Скачать последнюю версию](../../releases) |
| 📱 **AnimeLib Mobile (Android)** | [![Mobile Release](https://img.shields.io/github/v/release/Fazer000/AnimeLib-Mobile?label=mobile&style=flat-square)](https://github.com/Fazer000/AnimeLib-Mobile/releases) | [Скачать оригинальную мобильную версию](https://github.com/Fazer000/AnimeLib-Mobile/releases) |
| 🖥️ **AnimeLib Desktop (ПК)** | [![Desktop Release](https://img.shields.io/github/v/release/Fazer000/AnimeLib-Desktop?label=desktop&style=flat-square)](https://github.com/Fazer000/AnimeLib-Desktop/releases) | [Скачать PC версию](https://github.com/Fazer000/AnimeLib-Desktop/releases) |

---

## 🚀 Сборка и установка

### Предварительные требования
* **Android Studio** (Ladybug / Narwhal или новее)
* **JDK 17** или выше (рекомендуется JDK 21)
* **Android SDK**: Build-Tools 35.0.0, SDK Platform 35

### Инструкция по сборке

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/Fazer000/AnimeLib-Mobile.git
   cd AnimeLib-Mobile
   ```

2. Откройте проект в Android Studio и выполните синхронизацию Gradle.

3. Соберите Debug APK через консоль:
   ```bash
   gradle assembleDebug
   ```

4. Исполняемый файл APK будет находиться по пути:  
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 📁 Структура проекта

```
app/src/main/
├── java/com/example/animelib/
│   ├── adapters/          # Адаптеры для списков (видеофайлы, плейлисты, серии)
│   ├── controller/        # Контроллеры жестов, заморозки экрана, системного UI
│   ├── data/              # Room DB (Entities, DAOs, DatabaseManager)
│   ├── domain/            # Бизнес-логика и фильтры списков (VideoListFilter)
│   ├── managers/          # Менеджеры подсветки AmbientLight, 3D-звука, фильтров
│   ├── models/            # DTO и модели видеофайлов и плейлистов
│   ├── settings/          # BottomSheets настроек (скорость, субтитры, добавление в плейлист)
│   ├── ui/                # Кастомные View (M3ProgressIndicator, AmbientVignette)
│   ├── util/              # Вспомогательные утилиты, кэш миниатюр, тег-хелперы
│   ├── MainActivity.java  # Главный экран (Проводник + Плейлисты + Настройки)
│   └── VideoPlayerActivity.java # Экран кастомного видеоплеера AnimeLib
└── res/                   # Макеты layouts, ресурсы drawables, значения values
```

---

## 📄 Отказ от ответственности (Disclaimer)

* Данное приложение создано исключительно в ознакомительных и образовательных целях.
* Все логотипы, товарные знаки и графические материалы сайта **animelib.org** принадлежат **ООО «Мангалиб»**.
* Разработчик не несёт ответственности за контент, предоставляемый сторонними ресурсами и сервисами.

---

**Сайт и API**: [animelib.org](https://animelib.org) (ООО «Мангалиб»)  
**Оригинальный репозиторий AnimeLib Mobile**: [github.com/Fazer000/AnimeLib-Mobile](https://github.com/Fazer000/AnimeLib-Mobile)
