package com.pokechain.data.models

object Strings {
    fun filters(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Filters"; AppLanguage.ES -> "Filtros" }
    fun count(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Count"; AppLanguage.ES -> "Cantidad" }
    fun generate(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Generate"; AppLanguage.ES -> "Generar" }
    fun searchString(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Search String"; AppLanguage.ES -> "Cadena de búsqueda" }
    fun copy(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Copy"; AppLanguage.ES -> "Copiar" }
    fun copied(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Copied!"; AppLanguage.ES -> "Copiado" }
    fun close(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Close"; AppLanguage.ES -> "Cerrar" }
    fun copyError(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Copy error"; AppLanguage.ES -> "Copiar error" }
    fun apply(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Apply"; AppLanguage.ES -> "Aplicar" }
    fun xlCandy(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "XL Candy"; AppLanguage.ES -> "Caramelos XL" }
    fun shadowLabel(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Shadow"; AppLanguage.ES -> "Oscuro" }
    fun eliteMove(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Elite Move"; AppLanguage.ES -> "Mov. Élite" }
    fun unreleased(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Unreleased"; AppLanguage.ES -> "Sin liberar" }
    fun legendary(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Legendary"; AppLanguage.ES -> "Legendario" }
    fun megaPrimal(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Mega/Primal"; AppLanguage.ES -> "Mega/Primal" }
    fun tagShadow(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Shadow"; AppLanguage.ES -> "Oscuro" }
    fun tagElite(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "Elite"; AppLanguage.ES -> "Élite" }
    fun tagXL(lang: AppLanguage) = when (lang) { AppLanguage.EN -> "XL"; AppLanguage.ES -> "XL" }
    fun tagMega(lang: AppLanguage) = "Mega"

    fun leagueName(league: PvPLeague, lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> when (league) { PvPLeague.GREAT -> "Great"; PvPLeague.ULTRA -> "Ultra"; PvPLeague.MASTER -> "Master" }
        AppLanguage.ES -> when (league) { PvPLeague.GREAT -> "Super"; PvPLeague.ULTRA -> "Ultra"; PvPLeague.MASTER -> "Master" }
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
        0.1f to { "Descargando datos de PvPoke..." },
        0.3f to { when (this) { AppLanguage.EN -> "Downloading PvPoke..."; AppLanguage.ES -> "Descargando PvPoke..." } },
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
