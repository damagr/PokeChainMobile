![Version](https://img.shields.io/github/v/release/Damagr/PokeChainMobile?include_prereleases)
![License](https://img.shields.io/badge/license-MIT-green)

# PokeChain

Aplicación complementaria para Pokémon GO con generador de cadenas de búsqueda, pokédex, rankings de atacantes (PvE y Combates Max), calculadora IV, calculadora de exhibiciones y más.

> **Requisito:** Android 8.0 o superior y **conexión a internet** — los rankings y datos se descargan en vivo de [dialgadex.com](https://dialgadex.com), [pvpoke.com](https://pvpoke.com) y [dittobase.com](https://www.dittobase.com).

---

## Índice

| Sección | |
|---------|---|
| [Instalación](#instalación) | [Guía de uso](#guía-de-uso) |
| [Notas y preguntas frecuentes](#notas-y-preguntas-frecuentes) | [¿iPhone?](#iphone) |

---

## Instalación

1. Ve a [Releases](https://github.com/Damagr/PokeChainMobile/releases) y descarga el `app-release.apk` de la última versión.
2. Abre el APK descargado. Android pedirá permiso para instalar aplicaciones de origen desconocido → acéptalo para esta app.
3. Pulsa Instalar y abre PokeChain.

**Actualizaciones:** la propia app comprueba GitHub Releases al abrirla y te avisa cuando hay una versión nueva — se descarga e instala con un toque.

---

## Guía de uso

### Cambio de idioma

En la pantalla principal, arriba: selector **ES / EN**. Toda la app, incluidos los nombres de Pokémon, movimientos, ligas y formas, se traduce al instante.

### Cadena (PvP y PvE)

Genera **cadenas de búsqueda** listas para pegar en el cuadro de búsqueda de Pokémon GO.

**Cadena PvP:**
1. Pestaña **PvP**.
2. Elige liga o copa (Superball, Ultraball, Master y 29 copas temáticas).
3. Introduce el PC mínimo y pulsa **Generar**.
4. Copia la cadena y pégala en Pokémon GO.

Filtros: **Caramelos XL**, **Oscuro** y **Movimiento élite**.

**Cadena PvE:**
1. Pestaña **PvE**.
2. Elige tipo y filtros (inéditos, oscuro, oscuro casual, legendarios, mega/primigenio).
3. **Generar** → cadena con los mejores atacantes para ese tipo.

> La primera vez tarda un poco más: se descargan los datos de cálculo de dialgadex.

### Limpiar

Marca los atributos que quieres conservar (4★, 3★, variocolor, suerte, favoritos, disfraz, fondo de lugar, legendario, singular, ultraente, gigamax) → **Generar** → copia la cadena de limpieza.

### Pokédex (Tipos)

Busca un Pokémon por nombre o número para ver su **ficha**:

- Sprite oficial, tipos, **resistencias** y **debilidades**.
- **Rankings PvP** en las 32 ligas y copas (posición del Pokémon y sus formas, incluida la oscura).
- **Ranking PvE** global (top 300) y **por tipo** (top 25) con su mejor moveset.
- **Ataques:** arriba se muestra el **ataque cargado que tú eliges** (nivel normal); debajo, **"Mega nivel 4"** con el ataque supermega predefinido — el que lleva el signo **+** en la cadena de búsqueda de dialgadex.

### Top por Tipo

1. Elige **Global** o un **tipo**.
2. Lista de los mejores atacantes por eDPS (con etiquetas de Oscuro, Mega y Primigenio).
3. Interruptor **Mega** para incluir o excluir megas.
4. **Toca una tarjeta** → se abre la **ficha del Pokémon en la Pokédex**. Con **atrás** vuelves al ranking con tu tipo y resultados intactos.

### Top por Max

1. Elige un **tipo**.
2. Top 10 de atacantes para **Combates Max** (solo formas **Dinamax** y **Gigantamax**), ordenados por daño del movimiento Max, con su ataque rápido y su movimiento Max.
3. **Toca una tarjeta** → ficha en la Pokédex.

Datos de [dittobase.com](https://www.dittobase.com/pokemon-go/best-attackers/max-battles).

### Calculadora IV

1. Busca el Pokémon e introduce su **PC** actual y sus **IVs** (ataque, defensa, salud) → **Calcular**.
2. Verás el **nivel exacto**, el **% de perfección** y los **PS estimados** (puede haber varios niveles que den el mismo PC).
3. **Proyección:** mueve el slider hasta el nivel objetivo (hasta **51** con Mejor Amigo) → PC y PS proyectados y **coste** en polvo, caramelos y caramelos XL.
4. Marca **Costes Oscuro** si el Pokémon es **Oscuro**: los costes se sustituyen por los de un oscuro (+20% de recargo).

La tarjeta también genera un **nombre formateado** (ej. `Mewtwo⁹³`) copiable para renombrar en GO.

**Escáner en vivo (opcional):** toca el **icono de cámara** arriba a la derecha:

1. Concede los permisos: **"Mostrar sobre otras apps"** y **"Grabar pantalla"**.
2. Cambia a Pokémon GO, abre el Pokémon y entra en su **Valoración** (la pantalla de las barras).
3. El **chip flotante** mide las barras y muestra el % de perfección (ej. `⁹³`).
4. **Copiar** → pégalo como nombre del Pokémon.

El chip se puede **arrastrar**, **colapsar** (botón "–") y se oculta dentro de PokeChain.

### Calculadora de Exhibición

Introduce altura y peso del Pokémon (y IVs opcionalmente) → **puntuación de la Exhibición de PokéParada** (máx. 1178), medallas XXL/XXS, desglose por altura/peso/IVs y tier.

**Escáner en vivo (opcional):** el mismo **icono de cámara**, desde la pantalla de Exhibición:

1. Concede los permisos y cambia a Pokémon GO.
2. Abre el **Pokémon** (su página muestra el peso y la altura).
3. El chip muestra la puntuación lista para renombrar (ej. `987/1178`) → **Copiar**.
4. (Opcional) Abre la **Valoración** para sumar los puntos de IVs al score — el chip se actualiza al momento.

---

## Notas y preguntas frecuentes

**¿Necesita internet?**
Sí. Los rankings y datos se descargan en tiempo real de dialgadex, pvpoke y dittobase. La primera carga tarda más; después se cachean.

**¿Por qué hay ataques con "+"?**
Son los ataques **supermega** (nivel 4): el mejor en los rankings, pero **predefinidos** — no los eliges tú. Por eso la Pokédex muestra además el mejor ataque **elegible** (sin "+"), que sí puedes ponerle al Pokémon.

**¿Por qué mi Pokémon Oscuro cuesta más?**
Los Oscuros pagan un recargo del **+20%** en polvo y caramelos. Marca **Costes Oscuro** en la Calculadora IV para ver los costes exactos.

**¿Por qué no salen Megas en Top por Max?**
Un Pokémon Mega no puede Dinamaxear: en los Combates Max solo participan formas **Dinamax** y **Gigantamax**.

**¿Por qué la proyección llega al nivel 51?**
El nivel 51 se alcanza con el boost de **Mejor Amigo** (temporal y gratuito).

**Modo avanzado oculto**
Toca el icono de Fuecoco 5 veces en menos de 1.5 segundos para desbloquear el rango desde/hasta en las cadenas PvP y PvE.

**Tema oscuro**
Se adapta automáticamente al modo oscuro del sistema.

**"Android dice: ajuste restringido / acceso denegado"**
Las apps instaladas fuera de tiendas tienen los permisos sensibles bloqueados por Android. Desbloquéalo así: **Ajustes → Aplicaciones → PokeChain → menú ⋮ → Permitir permisos restringidos**. Después vuelve a la app y concede el permiso. (Alternativa: reinstalar por ADB.)

**¿Puedo ser baneado por el escáner?**
No. Solo lee tu propia pantalla (lo mismo que una captura manual): no interacciona con el juego, no inyecta nada ni automatiza nada.

**El chip tapa la altura, el peso o el teclado**
Arrástralo a otra posición con el dedo o colápsalo con el botón "–". En Exhibición, si tu compañero tapa la altura, tócalo para que se mueva e inténtalo de nuevo.

---

## ¿iPhone?

PokeChain es solo para Android. Si usas iPhone, puedes ejecutarlo en tu PC con el emulador oficial de Google:

1. Instala [Android Studio](https://developer.android.com/studio) (gratis, Windows/Mac/Linux)
2. Abre el Device Manager y crea un dispositivo virtual (ej. Pixel 6 con Android 14+)
3. Inicia el emulador y descarga el APK desde [GitHub Releases](https://github.com/Damagr/PokeChainMobile/releases)
4. Arrastra el APK a la ventana del emulador — se instala automáticamente

---

## Licencia

[MIT](LICENSE)

---

# English

Companion app for Pokémon GO with search string generator, pokédex, attacker rankings (PvE and Max Battles), IV calculator, showcase calculator and more.

> **Requirement:** Android 8.0 or higher and an **internet connection** — rankings and data are downloaded live from [dialgadex.com](https://dialgadex.com), [pvpoke.com](https://pvpoke.com) and [dittobase.com](https://www.dittobase.com).

## Index

| Section | |
|---------|---|
| [Installation](#installation-1) | [User guide](#user-guide) |
| [Notes & FAQ](#notes--faq) | [iPhone?](#iphone-2) |

---

## Installation

1. Go to [Releases](https://github.com/Damagr/PokeChainMobile/releases) and download the latest `app-release.apk`.
2. Open the APK. Android will ask for permission to install unknown apps → allow it for this app.
3. Tap Install and open PokeChain.

**Updates:** the app checks GitHub Releases on launch and notifies you when a new version is available — it downloads and installs with one tap.

---

## User guide

### Language switch

On the main screen, at the top: **ES / EN** selector. The whole app translates instantly, including Pokémon names, moves, leagues and forms.

### Chain (PvP and PvE)

Generates **search strings** ready to paste into the Pokémon GO search box.

**PvP Chain:**
1. **PvP** tab.
2. Pick a league or cup (Great, Ultra, Master and 29 themed cups).
3. Enter the minimum CP and tap **Generate**.
4. Copy the string and paste it into Pokémon GO.

Filters: **XL Candy**, **Shadow** and **Elite Move**.

**PvE Chain:**
1. **PvE** tab.
2. Pick a type and filters (unreleased, shadow, casual shadow, legendary, mega/primal).
3. **Generate** → string with the best attackers for that type.

> The first run takes a little longer: dialgadex calculation data is downloaded.

### Clean

Tick the attributes you want to keep (4★, 3★, Shiny, Lucky, Favorite, Costume, Location background, Legendary, Mythical, Ultra Beast, Gigantamax) → **Generate** → copy the cleanup string.

### Pokédex (Types)

Search a Pokémon by name or number to see its **detail page**:

- Official sprite, types, **resistances** and **weaknesses**.
- **PvP rankings** across 32 leagues and cups (position of the Pokémon and its forms, including Shadow).
- **PvE ranking** global (top 300) and **per type** (top 25) with its best moveset.
- **Attacks:** the top line shows the **charged attack you choose** (normal level); below, **"Mega level 4"** shows the predefined supermega attack — the one with the **+** sign in dialgadex search strings.

### Type Top

1. Pick **Global** or a **type**.
2. List of the best attackers by eDPS (with Shadow, Mega and Primal tags).
3. **Mega** switch to include or exclude megas.
4. **Tap a card** → the **Pokédex page opens**. With **back** you return to the ranking with your type and results intact.

### Max Top

1. Pick a **type**.
2. Top 10 attackers for **Max Battles** (only **Dynamax** and **Gigantamax** forms), sorted by Max Move damage, with their fast attack and Max Move.
3. **Tap a card** → Pokédex page.

Data from [dittobase.com](https://www.dittobase.com/pokemon-go/best-attackers/max-battles).

### IV Calculator

1. Search the Pokémon and enter its current **CP** and **IVs** (attack, defense, stamina) → **Calculate**.
2. You'll see the **exact level**, the **perfection %** and estimated **HP** (several levels can match the same CP).
3. **Projection:** move the slider to the target level (up to **51** with Best Buddy) → projected CP and HP plus the **cost** in dust, candy and XL candy.
4. Tick **Shadow costs** if the Pokémon is **Shadow**: costs switch to Shadow values (+20% surcharge).

The card also generates a copyable **formatted name** (e.g. `Mewtwo⁹³`) for renaming in GO.

**Live scanner (optional):** tap the **camera icon** at the top right:

1. Grant the permissions: **"Display over other apps"** and **"Record screen"**.
2. Switch to Pokémon GO, open the Pokémon and enter its **Appraisal** (the bars screen).
3. The **floating chip** measures the bars and shows the perfection % (e.g. `⁹³`).
4. **Copy** → paste it as the Pokémon's name.

The chip can be **dragged**, **collapsed** (the "–" button) and hides inside PokeChain.

### Showcase Calculator

Enter the Pokémon's height and weight (and IVs optionally) → the **PokéStop Showcase score** (max 1178), XXL/XXS badges, height/weight/IV breakdown and trophy tier.

**Live scanner (optional):** the same **camera icon**, from the Showcase screen:

1. Grant the permissions and switch to Pokémon GO.
2. Open the **Pokémon** (its page shows weight and height).
3. The chip shows the score ready to rename (e.g. `987/1178`) → **Copy**.
4. (Optional) Open the **Appraisal** to add the IV points to the score — the chip updates on the spot.

---

## Notes & FAQ

**Does it need internet?**
Yes. Rankings and data are downloaded in real time from dialgadex, pvpoke and dittobase. The first load takes longer; afterwards everything is cached.

**Why are there attacks with "+"?**
They are **supermega** attacks (level 4): the best in the rankings, but **predefined** — you don't choose them. That's why the Pokédex also shows the best **selectable** attack (without "+"), the one you can actually put on the Pokémon.

**Why does my Shadow Pokémon cost more?**
Shadow Pokémon pay a **+20%** surcharge on dust and candy. Tick **Shadow costs** in the IV Calculator for the exact values.

**Why don't Megas appear in Max Top?**
A Mega Pokémon cannot Dynamax: only **Dynamax** and **Gigantamax** forms take part in Max Battles.

**Why does the projection go up to level 51?**
Level 51 is reached with the **Best Buddy** boost (temporary and free).

**Hidden advanced mode**
Tap the Fuecoco icon 5 times within 1.5 seconds to unlock the from/to rank range in the PvP and PvE chains.

**Dark theme**
Automatically adapts to the system dark mode.

**"Android says: restricted setting / access denied"**
Apps installed outside stores have sensitive permissions blocked by Android. Unlock it: **Settings → Apps → PokeChain → ⋮ menu → Allow restricted permissions**. Then go back to the app and grant the permission. (Alternative: reinstall via ADB.)

**Can I get banned for the scanner?**
No. It only reads your own screen (just like a manual screenshot): it does not interact with the game, inject anything or automate anything.

**The chip covers the height, weight or keyboard**
Drag it to another position or collapse it with the "–" button. In Showcases, if your buddy covers the height, tap it so it moves and try again.

---

## iPhone?

PokeChain is Android-only. If you're on iPhone, you can run it on your PC using Google's official emulator:

1. Install [Android Studio](https://developer.android.com/studio) (free, Windows/Mac/Linux)
2. Open Device Manager and create a virtual device (e.g. Pixel 6 with Android 14+)
3. Launch the emulator and download the APK from [GitHub Releases](https://github.com/Damagr/PokeChainMobile/releases)
4. Drag the APK onto the emulator window — it installs automatically

---

## License

[MIT](LICENSE)