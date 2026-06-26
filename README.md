![Version](https://img.shields.io/github/v/release/Damagr/PokeChainMobile?include_prereleases)
![License](https://img.shields.io/badge/license-MIT-green)

# PokeChain

Aplicación complementaria para Pokémon GO con generador de cadenas de búsqueda, pokédex, calculadora IV, calculadora de exhibiciones y más.

---

## Características

- **Cadena PvP** — Rankings de [pvpoke.com](https://pvpoke.com) para 32 ligas y copas (Superball, Ultraball, Master y 29 copas). Filtros: Caramelos XL, Oscuro, Mov. Élite. Genera cadena de búsqueda lista para pegar en Pokémon GO.
- **Cadena PvE** — Rankings de atacantes de incursión de [dialgadex.com](https://dialgadex.com) por tipo. Filtros: Inéditos, Oscuro, Oscuro (casual), Legendarios, Mega/Primal. Usa WebView + scraping JS.
- **Limpiar** — Genera cadenas de filtro por 11 atributos (4★, 3★, variocolor, suerte, favoritos, disfraz, fondo lugar, legendario, singular, ultraente, gigamax).
- **Pokédex** — Buscador de Pokémon con tabla de tipos (debilidades/fortalezas), rankings PvP en las 32 ligas, ranking PvE global (top 300) y por tipo (top 25). Sprites oficiales desde PokeAPI.
- **Calculadora IV** — Encuentra el nivel exacto a partir de CP + IVs. Muestra % de perfección, proyección de CP hasta nivel 50, tabla de costes (polvo/caramelos/caramelos XL) y genera nombre formateado para GO.
- **Calculadora de Exhibición** — Calcula la puntuación de PokéStop Showcase (máx. 1178). Medallas XXL/XXS, desglose por altura/peso/IVs y tier de trofeo.
- **Auto-actualizador** — Revisa GitHub Releases en busca de nuevos APKs y los instala con un solo toque.
- **Bilingüe** — Traducción completa al español e inglés (nombres de Pokémon, movimientos, ligas y formularios incluidos).
- **Modo avanzado oculto** — Toca el icono de Fuecoco 5 veces en menos de 1.5 segundos para desbloquear rango desde/hasta en PvP y PvE.
- **Tema oscuro** — Se adapta automáticamente al modo oscuro del sistema.

## Stack tecnológico

| Categoría    | Tecnología |
|--------------|------------|
| Lenguaje     | Kotlin |
| UI           | Jetpack Compose + Material 3 |
| Red          | OkHttp 4 + Kotlinx Serialization |
| Imágenes     | Coil |
| Concurrencia | Kotlinx Coroutines |
| Compile SDK  | 37 (Android 17) |
| Min SDK      | 26 (Android 8.0) |
| Target SDK   | 37 (Android 17) |

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

> Requiere JDK 17 y Android SDK 37.

## Licencia

[MIT](LICENSE)

---

# English

Companion app for Pokémon GO with search string generator, pokédex, IV calculator, showcase calculator and more.

## Features

- **PvP Chain** — Rankings from [pvpoke.com](https://pvpoke.com) for 32 leagues and cups (Great, Ultra, Master and 29 cups). Filters: XL Candy, Shadow, Elite Move. Generates a search string ready to paste into Pokémon GO.
- **PvE Chain** — Raid attacker rankings from [dialgadex.com](https://dialgadex.com) by type. Filters: Unreleased, Shadow, Shadow (casual), Legendary, Mega/Primal. Uses WebView + JS scraping.
- **Clean** — Generates filter strings by 11 attributes (4★, 3★, Shiny, Lucky, Favorite, Costume, Location background, Legendary, Mythical, Ultra Beast, Gigantamax).
- **Pokédex** — Pokémon lookup with type chart (weakness/resistance), PvP rankings across 32 leagues, global PvE ranking (top 300) and per-type PvE ranking (top 25). Official sprites from PokeAPI.
- **IV Calculator** — Finds exact level from CP + IVs. Shows perfection %, CP projection up to level 50, power-up cost table (dust/candy/XL candy) and generates a formatted GO name.
- **Showcase Calculator** — Computes PokéStop Showcase score (max 1178). XXL/XXS badges, height/weight/IV breakdown and trophy tier.
- **Auto-updater** — Checks GitHub Releases for new APKs and installs them with one tap.
- **Bilingual** — Full English and Spanish translations (Pokémon names, moves, leagues and forms included).
- **Hidden advanced mode** — Tap the Fuecoco icon 5 times within 1.5 seconds to unlock from/to rank range for PvP and PvE.
- **Dark theme** — Automatically adapts to the system dark mode.

## Tech Stack

| Category     | Technology |
|--------------|------------|
| Language     | Kotlin |
| UI           | Jetpack Compose + Material 3 |
| Networking   | OkHttp 4 + Kotlinx Serialization |
| Images       | Coil |
| Concurrency  | Kotlinx Coroutines |
| Compile SDK  | 37 (Android 17) |
| Min SDK      | 26 (Android 8.0) |
| Target SDK   | 37 (Android 17) |

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

> Requires JDK 17 and Android SDK 37.

## License

[MIT](LICENSE)
