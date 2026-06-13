![Version](https://img.shields.io/badge/version-1.1.2--beta-blue)
![License](https://img.shields.io/badge/license-MIT-green)

# PokeChain

Aplicación complementaria para Pokémon GO que genera cadenas de búsqueda para filtrar rápidamente tu almacenamiento Pokémon por relevancia PvP o PvE.

---

## Características

- **PvP** — Obtiene rankings de [pvpoke.com](https://pvpoke.com) para las ligas Superball, Ultraball y Master. Aplica filtros (Caramelos XL, Oscuro, Mov. Élite) y genera una cadena de búsqueda lista para pegar en Pokémon GO.
- **PvE** — Extrae rankings de atacantes de incursión de [dialgadex.com](https://dialgadex.com) por tipo y genera cadenas de búsqueda. Filtros disponibles: Inéditos, Oscuro, Oscuro (jugador casual), Legendarios, Mega/Primal.
- **Limpiar** — Pestaña de utilidad para generar cadenas de filtro por atributos (4★, 3★, variocolor, suerte, favoritos, disfraz, fondo lugar, legendario, singular, ultraente, gigamax).
- **Auto-actualizador** — Revisa GitHub Releases en busca de nuevos APKs y los instala con un solo toque.
- **Bilingüe** — Traducción completa al español e inglés.
- **Modo avanzado oculto** — Toca el icono de Fuecoco 5 veces en menos de 1.5 segundos para desbloquear controles extra (rango desde/hasta para PvP y PvE).

## Stack tecnológico

| Categoría  | Tecnología |
|------------|------------|
| Lenguaje   | Kotlin |
| UI         | Jetpack Compose + Material 3 |
| Red        | OkHttp 4 + Gson + Kotlinx Serialization |
| Concurrencia | Kotlinx Coroutines |
| Min SDK    | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

## Compilar

1. Clona el repositorio:
   ```bash
   git clone https://github.com/Damagr/PokeChainMobile.git
   ```
2. Abre en Android Studio **o** compila desde terminal:
   ```bash
   ./gradlew assembleDebug
   ```
3. El APK estará en `app/build/outputs/apk/debug/`.

> Requiere JDK 17 y Android SDK 35.

## Licencia

[MIT](LICENSE)

---

# English

Companion app for Pokémon GO that generates search strings so you can quickly filter your Pokémon storage by PvP or PvE relevance.

## Features

- **PvP** — Pulls rankings from [pvpoke.com](https://pvpoke.com) for Great, Ultra, and Master League. Apply filters (XL Candy, Shadow, Elite Move) and get a comma-separated search string ready to paste into Pokémon GO.
- **PvE** — Scrapes raid attacker rankings from [dialgadex.com](https://dialgadex.com) by type and outputs search strings. Available filters: Unreleased, Shadow, Shadow (casual player), Legendary, Mega/Primal.
- **Clean** — Utility tab to generate filter strings by attributes (4★, 3★, Shiny, Lucky, Favorite, Costume, Location background, Legendary, Mythical, Ultra Beast, Gigantamax).
- **Auto-updater** — Checks GitHub Releases for new APKs and installs them with one tap.
- **Bilingual** — Full English and Spanish translations.
- **Hidden advanced mode** — Tap the Fuecoco icon 5 times within 1.5 seconds to unlock extra controls (from/to rank range for PvP and PvE).

## Tech Stack

| Category   | Technology |
|------------|------------|
| Language   | Kotlin |
| UI         | Jetpack Compose + Material 3 |
| Networking | OkHttp 4 + Gson + Kotlinx Serialization |
| Concurrency | Kotlinx Coroutines |
| Min SDK    | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

## Build

1. Clone the repo:
   ```bash
   git clone https://github.com/Damagr/PokeChainMobile.git
   ```
2. Open in Android Studio **or** build from CLI:
   ```bash
   ./gradlew assembleDebug
   ```
3. The APK will be at `app/build/outputs/apk/debug/`.

> Requires JDK 17 and Android SDK 35.

## License

[MIT](LICENSE)
