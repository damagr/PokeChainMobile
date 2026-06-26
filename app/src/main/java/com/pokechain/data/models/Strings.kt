package com.pokechain.data.models

object Strings {
    fun filters(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Filters"; AppLanguage.ES -> "Filtros" }
    fun topCount(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Results"; AppLanguage.ES -> "Mostrar" }
    fun fromRank(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "From"; AppLanguage.ES -> "Desde" }
    fun toRank(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "To"; AppLanguage.ES -> "Hasta" }
    fun generate(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Generate"; AppLanguage.ES -> "Generar" }
    fun topPvP(lang: AppLanguage, count: Int, league: String) = when (lang) {
        AppLanguage.EN -> "Top $count $league League — PvP"
        AppLanguage.ES -> "Top $count Liga $league — PvP"
    }
    fun topPvPRange(lang: AppLanguage, from: Int, to: Int, league: String) = when (lang) {
        AppLanguage.EN -> "Top $from–$to $league League — PvP"
        AppLanguage.ES -> "Top $from–$to Liga $league — PvP"
    }
    fun topPvE(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.EN -> "Top $count PvE Attackers"
        AppLanguage.ES -> "Top $count Atacantes PvE"
    }
    fun topPvERange(lang: AppLanguage, from: Int, to: Int) = when (lang) {
        AppLanguage.EN -> "Top $from–$to PvE Attackers"
        AppLanguage.ES -> "Top $from–$to Atacantes PvE"
    }
    fun searchString(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Search String"; AppLanguage.ES -> "Cadena de búsqueda" }
    fun copy(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Copy"; AppLanguage.ES -> "Copiar" }
    fun copied(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Copied!"; AppLanguage.ES -> "Copiado" }
    fun close(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Close"; AppLanguage.ES -> "Cerrar" }
    fun cancel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Cancel"; AppLanguage.ES -> "Cancelar" }
    fun copyError(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Copy error"; AppLanguage.ES -> "Copiar error" }
    fun apply(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Apply"; AppLanguage.ES -> "Aplicar" }
    fun xlCandy(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "XL Candy"; AppLanguage.ES -> "Caramelos XL" }
    fun shadowLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Shadow"; AppLanguage.ES -> "Oscuro" }
    fun eliteMove(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Elite Move"; AppLanguage.ES -> "Mov. Élite" }
    fun cleanTab(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Clean"; AppLanguage.ES -> "Limpiar" }
    fun chainSection(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Chain"; AppLanguage.ES -> "Cadena" }
    fun typesSection(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Pokédex"; AppLanguage.ES -> "Pokédex" }
    fun typeSearch(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Search Pokémon…"; AppLanguage.ES -> "Buscar Pokémon…" }
    fun resistantTo(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Resistant to"; AppLanguage.ES -> "Resistente a" }
    fun weakTo(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Weak to"; AppLanguage.ES -> "Débil contra" }
    fun immuneTo(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Immune to"; AppLanguage.ES -> "Inmune a" }
    fun counters(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Counters"; AppLanguage.ES -> "Counters" }
    fun noPokemonSelected(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Search for a Pokémon to see its type info"; AppLanguage.ES -> "Busca un Pokémon para ver sus tipos" }
    fun back(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Back"; AppLanguage.ES -> "Atrás" }
    fun pokemonNumber(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "No."; AppLanguage.ES -> "N.º" }
    fun unreleased(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Unreleased"; AppLanguage.ES -> "Inédito" }
    fun legendary(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Legendary"; AppLanguage.ES -> "Legendario" }
    fun megaPrimal(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Mega/Primal"; AppLanguage.ES -> "Mega/Primigenio" }
    fun casualShadow(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Shadow (casual player)"; AppLanguage.ES -> "Oscuro (jugador casual)" }
    fun tagShadow(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Shadow"; AppLanguage.ES -> "Oscuro" }
    fun tagElite(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Elite"; AppLanguage.ES -> "Élite" }
    fun tagXL(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "XL"; AppLanguage.ES -> "XL" }
    fun tagMega(lang: AppLanguage) = "Mega"
    fun maxCount(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Max 300"; AppLanguage.ES -> "Máx. 300" }
    fun enterCount(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Enter a number"; AppLanguage.ES -> "Introduce un número" }
    fun enterPositiveCount(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Enter a positive number of Pokémon to display"
        AppLanguage.ES -> "Introduce una cantidad positiva de Pokémon a mostrar"
    }
    fun enterPositiveRange(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Enter a valid range (From < To, max 300)"
        AppLanguage.ES -> "Introduce un rango válido (Desde < Hasta, máx 300)"
    }
    fun advancedModeOn(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Advanced mode ON"
        AppLanguage.ES -> "Modo avanzado activado"
    }
    fun advancedModeOff(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Advanced mode OFF"
        AppLanguage.ES -> "Modo avanzado desactivado"
    }
    fun newVersionAvailable(lang: AppLanguage, latest: String) = when (lang) {
        AppLanguage.EN -> "New version $latest available!"
        AppLanguage.ES -> "¡Nueva versión $latest disponible!"
    }
    fun download(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Download"; AppLanguage.ES -> "Descargar" }
    fun later(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Later"; AppLanguage.ES -> "Más tarde" }
    fun updateLink(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Open releases page to download the new version."
        AppLanguage.ES -> "Abre la página de versiones para descargar la nueva versión."
    }
    fun downloading(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Downloading..."; AppLanguage.ES -> "Descargando..." }
    fun installing(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Installing..."; AppLanguage.ES -> "Instalando..." }
    fun updateFailed(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Download failed"; AppLanguage.ES -> "Fallo al descargar" }

    fun leagueName(league: PvPLeague, lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> when (league) {
            PvPLeague.GREAT -> "Great"; PvPLeague.ULTRA -> "Ultra"; PvPLeague.MASTER -> "Master"
            PvPLeague.SUNSHINE -> "Sunshine"; PvPLeague.CATCH -> "Catch"; PvPLeague.COLOR -> "Color"
            PvPLeague.ELEMENT -> "Element"; PvPLeague.EVOLUTION -> "Evolution"
            PvPLeague.FANTASY -> "Fantasy"; PvPLeague.ULTRA_FANTASY -> "Ultra Fantasy"
            PvPLeague.FIGHTING -> "Fighting"; PvPLeague.FLYING -> "Flying"; PvPLeague.FOSSIL -> "Fossil"
            PvPLeague.HALLOWEEN -> "Halloween"; PvPLeague.ULTRA_HALLOWEEN -> "Ultra Halloween"
            PvPLeague.HISUI -> "Hisui"; PvPLeague.JUNGLE -> "Jungle"; PvPLeague.KANTO -> "Kanto"
            PvPLeague.LOVE -> "Love"; PvPLeague.MOUNTAIN -> "Mountain"
            PvPLeague.PREMIER -> "Premier"; PvPLeague.ULTRA_PREMIER -> "Ultra Premier"
            PvPLeague.PSYCHIC -> "Psychic"
            PvPLeague.REMIX -> "Remix"; PvPLeague.ULTRA_REMIX -> "Ultra Remix"
            PvPLeague.RETRO -> "Retro"; PvPLeague.SPRING -> "Spring"
            PvPLeague.SUMMER -> "Summer"; PvPLeague.ULTRA_SUMMER -> "Ultra Summer"
            PvPLeague.WEATHER -> "Weather"; PvPLeague.ULTRA_WEATHER -> "Ultra Weather"
            PvPLeague.WILLPOWER -> "Willpower"
        }
        AppLanguage.ES -> when (league) {
            PvPLeague.GREAT -> "Super"; PvPLeague.ULTRA -> "Ultra"; PvPLeague.MASTER -> "Master"
            PvPLeague.SUNSHINE -> "Luz Solar"; PvPLeague.CATCH -> "Captura"; PvPLeague.COLOR -> "Color"
            PvPLeague.ELEMENT -> "Elemental"; PvPLeague.EVOLUTION -> "Evolución"
            PvPLeague.FANTASY -> "Fantasía"; PvPLeague.ULTRA_FANTASY -> "Ultra Fantasía"
            PvPLeague.FIGHTING -> "Lucha"; PvPLeague.FLYING -> "Volador"; PvPLeague.FOSSIL -> "Fósil"
            PvPLeague.HALLOWEEN -> "Halloween"; PvPLeague.ULTRA_HALLOWEEN -> "Ultra Halloween"
            PvPLeague.HISUI -> "Hisui"; PvPLeague.JUNGLE -> "Jungla"; PvPLeague.KANTO -> "Kanto"
            PvPLeague.LOVE -> "Amor"; PvPLeague.MOUNTAIN -> "Montaña"
            PvPLeague.PREMIER -> "Premier"; PvPLeague.ULTRA_PREMIER -> "Ultra Premier"
            PvPLeague.PSYCHIC -> "Psíquico"
            PvPLeague.REMIX -> "Remix"; PvPLeague.ULTRA_REMIX -> "Ultra Remix"
            PvPLeague.RETRO -> "Retro"; PvPLeague.SPRING -> "Primavera"
            PvPLeague.SUMMER -> "Verano"; PvPLeague.ULTRA_SUMMER -> "Ultra Verano"
            PvPLeague.WEATHER -> "Clima"; PvPLeague.ULTRA_WEATHER -> "Ultra Clima"
            PvPLeague.WILLPOWER -> "Voluntad"
        }
    }

    fun errorTitle(screen: String, lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "$screen Error"
        AppLanguage.ES -> "Error $screen"
    }

    fun progress(lang: AppLanguage, vararg msgs: String): (Int) -> String {
        val map = msgs.mapIndexed { i, m -> i to m }.toMap()
        return { stage -> map[stage] ?: "" }
    }

    val pvpProgress: List<Pair<Float, AppLanguage.() -> String>> = listOf(
        0.1f to { when (this) { AppLanguage.EN -> "Downloading PvPoke..."; AppLanguage.ES -> "Descargando datos de PvPoke..." } },
        0.3f to { when (this) { AppLanguage.EN -> "Downloading rankings..."; AppLanguage.ES -> "Descargando rankings..." } },
        0.5f to { when (this) { AppLanguage.EN -> "Processing rankings..."; AppLanguage.ES -> "Procesando rankings..." } },
        0.7f to { when (this) { AppLanguage.EN -> "Resolving base forms..."; AppLanguage.ES -> "Resolviendo formas base..." } },
        0.9f to { when (this) { AppLanguage.EN -> "Generating search string..."; AppLanguage.ES -> "Generando cadena de búsqueda..." } },
        1.0f to { when (this) { AppLanguage.EN -> "Complete"; AppLanguage.ES -> "Completado" } },
    )

    // ── IV Calculator ──────────────────────────────────────────────
    fun ivCalcSection(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "IV Calc"; AppLanguage.ES -> "Calc. IV" }
    fun ivCalculator(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "IV Calculator"; AppLanguage.ES -> "Calculadora IV" }
    fun ivComingSoon(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Coming soon"; AppLanguage.ES -> "Próximamente" }
    fun ivSelectPokemon(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Select Pokémon…"; AppLanguage.ES -> "Buscar Pokémon…" }
    fun ivCpLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "CP (Combat Points)"; AppLanguage.ES -> "PC (Puntos de Combate)" }
    fun ivAtkLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "ATK IV"; AppLanguage.ES -> "IV Ataque" }
    fun ivDefLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "DEF IV"; AppLanguage.ES -> "IV Defensa" }
    fun ivHpLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "HP IV"; AppLanguage.ES -> "IV Salud" }
    fun ivCalculate(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Calculate"; AppLanguage.ES -> "Calcular" }
    fun ivPerfection(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "IV Perfection"; AppLanguage.ES -> "Perfección IV" }
    fun ivLevel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Level"; AppLanguage.ES -> "Nivel" }
    fun ivEstimatedHp(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Estimated HP"; AppLanguage.ES -> "PS Estimados" }
    fun ivNoResults(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "No level found with these values. Check CP and IVs."
        AppLanguage.ES -> "No se encontró nivel con esos valores. Revisa PC e IVs."
    }
    fun ivInsertValid(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Enter valid values (IVs 0–15, CP ≥ 10)"
        AppLanguage.ES -> "Introduce valores válidos (IVs 0–15, PC ≥ 10)"
    }
    fun ivSelectFirst(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Select a Pokémon first"
        AppLanguage.ES -> "Selecciona un Pokémon primero"
    }
    fun ivCheckStats(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Check IV perfection & level"
        AppLanguage.ES -> "Calcula perfección IV y nivel"
    }
    // ── CP Projection ─────────────────────────────────────────────
    fun ivProjection(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "CP Projection"; AppLanguage.ES -> "Proyección de PC" }
    fun ivTargetLevel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Target Level"; AppLanguage.ES -> "Nivel objetivo" }
    fun ivProjectedCp(lang: AppLanguage) = "PC"
    fun ivProjectedHp(lang: AppLanguage) = "PS"
    fun ivCost(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Cost"; AppLanguage.ES -> "Coste" }
    fun ivDust(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Dust"; AppLanguage.ES -> "Polvo" }
    fun ivCandyLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Candy"; AppLanguage.ES -> "Caramelos" }
    fun ivXlCandyLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "XL Candy"; AppLanguage.ES -> "Caram. XL" }
    fun ivGoName(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "GO Name"; AppLanguage.ES -> "Nombre GO" }

    // ── Showcase Calculator ─────────────────────────────────────────
    fun showcaseSection(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Showcase"; AppLanguage.ES -> "Exhibición" }
    fun showcaseCalculator(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Showcase Calc"; AppLanguage.ES -> "Calc. Exhibición" }
    fun showcaseSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Score PokéStop Showcases"
        AppLanguage.ES -> "Calcula puntos de exhibición"
    }
    fun showcaseSelectPokemon(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Select Pokémon…"; AppLanguage.ES -> "Buscar Pokémon…" }
    fun showcaseHeight(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Height (m)"; AppLanguage.ES -> "Altura (m)" }
    fun showcaseWeight(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Weight (kg)"; AppLanguage.ES -> "Peso (kg)" }
    fun showcaseIvsOptional(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "IVs (optional)"; AppLanguage.ES -> "IVs (opcional)" }
    fun showcaseCalculate(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Calculate"; AppLanguage.ES -> "Calcular" }
    fun showcaseEnterValid(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Enter valid height, weight, and select a Pokémon first"
        AppLanguage.ES -> "Introduce altura, peso válidos y selecciona un Pokémon"
    }
    fun showcaseScore(lang: AppLanguage) = "Score"
    fun showcaseMax(lang: AppLanguage) = "/ 1178"
    fun showcaseBreakdown(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Breakdown"; AppLanguage.ES -> "Desglose" }
    fun showcaseIvLabel(lang: AppLanguage) = "IVs"
    fun showcaseXxlBonus(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "XXL Bonus"; AppLanguage.ES -> "Bonus XXL" }
    fun showcaseRatio(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "× base height"; AppLanguage.ES -> "× altura base" }
    fun showcaseNoData(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "No size data for this Pokémon"
        AppLanguage.ES -> "Sin datos de tamaño para este Pokémon"
    }
    fun showcaseTrophy(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Trophy class — near the cap"; AppLanguage.ES -> "Clase trofeo — casi imbatible" }
    fun showcaseExcellent(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Excellent — wins most showcases"; AppLanguage.ES -> "Excelente — gana la mayoría" }
    fun showcaseGood(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Good — competitive"; AppLanguage.ES -> "Bueno — competitivo" }
    fun showcaseBelow(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Below par — XXL height wins"; AppLanguage.ES -> "Por debajo — la altura XXL gana" }

    // ── Pokédex PvP / PvE ───────────────────────────────────────
    fun pvpLeagues(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Leagues & Cups"; AppLanguage.ES -> "Ligas y Copas" }
    fun pvpTopN(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Top 100"; AppLanguage.ES -> "Top 100" }
    fun pvpOut(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "—"; AppLanguage.ES -> "—" }
    fun pvpLoading(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Loading leagues…"; AppLanguage.ES -> "Cargando ligas…" }
    fun pvpLoadAll(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Load all cups"; AppLanguage.ES -> "Cargar todas las copas" }
    fun pveRanking(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "PvE Ranking"; AppLanguage.ES -> "Ranking PvE" }
    fun pveTopN(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Top 200"; AppLanguage.ES -> "Top 200" }
    fun pveNormal(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Normal"; AppLanguage.ES -> "Normal" }
    fun pveShadow(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Shadow"; AppLanguage.ES -> "Oscuro" }
    fun pveNotRanked(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Not ranked"; AppLanguage.ES -> "No existe" }
    fun pveOutOfClass(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Out of ranking"; AppLanguage.ES -> "Fuera de la clasificación" }
    fun pveLoading(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Loading PvE…"; AppLanguage.ES -> "Cargando PvE…" }
    fun pveBestMoveset(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Best Moveset"; AppLanguage.ES -> "Mejor Moveset" }
    fun retry(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Retry"; AppLanguage.ES -> "Reintentar" }
    fun pveInitEngine(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Initializing PvE engine… (may take up to 60s)"
        AppLanguage.ES -> "Inicializando motor PvE… (puede tardar hasta 60s)"
    }
    fun pveComputing(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Computing rankings (top 200)…"
        AppLanguage.ES -> "Calculando rankings (top 200)…"
    }
    fun pveTapToLoad(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Tap to load PvE rankings"
        AppLanguage.ES -> "Pulsa para cargar ranking PvE"
    }
    fun pveLoadButton(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Load PvE Ranking"
        AppLanguage.ES -> "Cargar ranking PvE"
    }

    // ── Pokédex type ranking ───────────────────────────────────
    fun pveTypeRanking(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Type Ranking (Top 25)"; AppLanguage.ES -> "Ranking por Tipo (Top 25)" }
    fun pveTypeTop25(lang: AppLanguage) = "Top 25"
    fun pveTypeLoad(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Load"; AppLanguage.ES -> "Cargar" }
    fun pveTypeTapToLoad(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Tap to load"
        AppLanguage.ES -> "Pulsa para cargar"
    }
    fun pveTypeOutOfRanking(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Not in type top 25"
        AppLanguage.ES -> "Fuera del top 25 del tipo"
    }

    val pveProgress: List<Pair<Float, AppLanguage.() -> String>> = listOf(
        0.1f to { when (this) { AppLanguage.EN -> "Downloading game data..."; AppLanguage.ES -> "Descargando datos del juego..." } },
        0.3f to { when (this) { AppLanguage.EN -> "Initializing PvE calculator..."; AppLanguage.ES -> "Inicializando calculadora PvE..." } },
        0.5f to { when (this) { AppLanguage.EN -> "Downloading game master..."; AppLanguage.ES -> "Descargando game master..." } },
        0.7f to { when (this) { AppLanguage.EN -> "Resolving base forms..."; AppLanguage.ES -> "Resolviendo formas base..." } },
        0.9f to { when (this) { AppLanguage.EN -> "Generating search string..."; AppLanguage.ES -> "Generando cadena de búsqueda..." } },
        1.0f to { when (this) { AppLanguage.EN -> "Complete"; AppLanguage.ES -> "Completado" } },
    )
}
