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
    fun typesSection(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Types"; AppLanguage.ES -> "Tipos" }
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
        AppLanguage.EN -> when (league) { PvPLeague.GREAT -> "Great"; PvPLeague.ULTRA -> "Ultra"; PvPLeague.MASTER -> "Master"; PvPLeague.SUNSHINE -> "Sunshine" }
        AppLanguage.ES -> when (league) { PvPLeague.GREAT -> "Super"; PvPLeague.ULTRA -> "Ultra"; PvPLeague.MASTER -> "Master"; PvPLeague.SUNSHINE -> "Sunshine" }
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

    val pveProgress: List<Pair<Float, AppLanguage.() -> String>> = listOf(
        0.1f to { when (this) { AppLanguage.EN -> "Downloading game data..."; AppLanguage.ES -> "Descargando datos del juego..." } },
        0.3f to { when (this) { AppLanguage.EN -> "Initializing PvE calculator..."; AppLanguage.ES -> "Inicializando calculadora PvE..." } },
        0.5f to { when (this) { AppLanguage.EN -> "Downloading game master..."; AppLanguage.ES -> "Descargando game master..." } },
        0.7f to { when (this) { AppLanguage.EN -> "Resolving base forms..."; AppLanguage.ES -> "Resolviendo formas base..." } },
        0.9f to { when (this) { AppLanguage.EN -> "Generating search string..."; AppLanguage.ES -> "Generando cadena de búsqueda..." } },
        1.0f to { when (this) { AppLanguage.EN -> "Complete"; AppLanguage.ES -> "Completado" } },
    )
}
