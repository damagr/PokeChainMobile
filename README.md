![Version](https://img.shields.io/badge/version-1.1.2--beta-blue)
![License](https://img.shields.io/badge/license-MIT-green)

# PokeChain

Companion app for Pokémon GO that generates search strings so you can quickly filter your Pokémon storage by PvP or PvE relevance.

## Features

- **PvP** — Pulls rankings from [pvpoke.com](https://pvpoke.com) for Great, Ultra, and Master League. Apply filters (XL Candy, Shadow, Elite Moves, Legendary, Mega/Primal, Unreleased) and get a comma-separated search string ready to paste into Pokémon GO.
- **PvE** — Scrapes raid attacker rankings from [dialgadex.com](https://dialgadex.com) by type and outputs the same kind of search strings.
- **Clean** — Utility tab for managing saved strings.
- **Auto-updater** — Checks GitHub Releases for new APKs and installs them with one tap.
- **Bilingual** — Full English and Spanish translations.
- **Hidden advanced mode** — Tap the app title 5 times to unlock extra controls.

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
