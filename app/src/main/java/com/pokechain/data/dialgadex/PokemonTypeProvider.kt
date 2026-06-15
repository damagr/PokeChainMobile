package com.pokechain.data.dialgadex

import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.PokemonType
import com.pokechain.data.models.TypeChart
import com.pokechain.data.pvpoke.PvPokeApi

data class PokemonTypeEntry(
    val speciesId: String,
    val dex: Int,
    val name: String,
    val types: List<PokemonType>
) {
    val isBaseForm: Boolean get() = !speciesId.contains("_")

    /** Returns the localized display name, translating form suffixes when possible. */
    fun displayName(language: AppLanguage, translator: NameTranslator): String {
        if (language == AppLanguage.EN) return name
        val baseName = translator.getName(dex, language)
        if (isBaseForm) return baseName

        // Try to extract form from speciesId and translate it
        val suffix = speciesId.substringAfter("_", "")
        val translatedForm = formTranslations[suffix]
        return if (translatedForm != null) "$baseName ($translatedForm)" else name
    }

    companion object {
        private val formTranslations = mapOf(
            // Alolan forms
            "alola" to "Alola",
            // Galarian
            "galarian" to "Galar",
            // Hisuian
            "hisuian" to "Hisui",
            // Paldean
            "paldean" to "Paldea",
            // Mega
            "mega" to "Mega",
            "mega_x" to "Mega X",
            "mega_y" to "Mega Y",
            // Primal / Primigenio
            "primal" to "Primigenio",
            // Origin
            "origin" to "Origen",
            // Altered / Modificada
            "altered" to "Modificada",
            // Therian / Tótem
            "therian" to "Tótem",
            // Incarnate / Avatar
            "incarnate" to "Avatar",
            // Crowned Sword / Espada Suprema
            "crowned_sword" to "Espada Suprema",
            "crowned_shield" to "Escudo Supremo",
            // Hero / Héroe
            "hero" to "Héroe",
            // Dusk Mane / Melena Crepuscular
            "dusk_mane" to "Melena Crepuscular",
            // Dawn Wings / Alas del Alba
            "dawn_wings" to "Alas del Alba",
            // Ultra / Ultra
            "ultra" to "Ultra",
            // Black / Negro
            "black" to "Negro",
            // White / Blanco
            "white" to "Blanco",
            // Kyurem forms
            "black_kyurem" to "Negro",
            "white_kyurem" to "Blanco",
            // Sky / Cielo (Shaymin)
            "sky" to "Cielo",
            // Pirouette / Brío (Meloetta)
            "pirouette" to "Brío",
            // Aria / Lírica (Meloetta)
            "aria" to "Lírica",
            // Blade / Filo (Aegislash)
            "blade" to "Filo",
            // Shield / Escudo (Aegislash)
            "shield" to "Escudo",
            // Sandy / Arena (Wormadam)
            "sandy" to "Arena",
            // Trash / Basura (Wormadam)
            "trash" to "Basura",
            // Plant / Planta (Wormadam)
            "plant" to "Planta",
            // Sunny / Soleado (Cherrim)
            "sunny" to "Soleado",
            // Overcast / Nublado (Cherrim)
            "overcast" to "Nublado",
            // Rainy / Lluvia (Castform)
            "rainy" to "Lluvia",
            // Snowy / Nieve (Castform)
            "snowy" to "Nieve",
            // Heat / Calor (Rotom)
            "heat" to "Calor",
            // Wash / Lavado (Rotom)
            "wash" to "Lavado",
            // Frost / Frío (Rotom)
            "frost" to "Frío",
            // Fan / Ventilador (Rotom)
            "fan" to "Ventilador",
            // Mow / Corte (Rotom)
            "mow" to "Corte",
            // Rapid Strike / Fluido (Urshifu)
            "rapid_strike" to "Fluido",
            // Single Strike / Brusco (Urshifu)
            "single_strike" to "Brusco",
            // Resolute / Resuelta (Keldeo)
            "resolute" to "Resuelta",
            // Unbound / Desatada (Hoopa)
            "unbound" to "Desatada",
            // Confined / Contenida (Hoopa)
            "confined" to "Contenida",
            // Douse / Calmante (Genesect)
            "douse" to "Calmante",
            // Shock / Fulminante (Genesect)
            "shock" to "Fulminante",
            // Burn / Incendiario (Genesect)
            "burn" to "Incendiario",
            // Chill / Gélido (Genesect)
            "chill" to "Gélido",
            // Normal / Normal (Deoxys, etc.)
            "normal" to "Normal",
            // Attack / Ataque (Deoxys)
            "attack" to "Ataque",
            // Defense / Defensa (Deoxys)
            "defense" to "Defensa",
            // Speed / Velocidad (Deoxys)
            "speed" to "Velocidad",
            // Complete / Completa (Zygarde)
            "complete" to "Completa",
            // X / X (Xerneas, Charizard)
            "x" to "X",
            // Y / Y (Yveltal, Charizard)
            "y" to "Y",
            // Shadow (for Giratina, etc.)
            "shadow" to "Sombra",
            // Land / Tierra (Tornadus/Thundurus/Landorus)
            "land" to "Tierra",
            // Size forms (Pumpkaboo, Gourgeist)
            "small" to "Pequeño",
            "large" to "Grande",
            "super" to "Súper",
            // Indeedee
            "male" to "Macho",
            "female" to "Hembra",
            // Meowstic
            "meowstic_male" to "Macho",
            "meowstic_female" to "Hembra",
            // Indeedee specific
            "indeedee_male" to "Macho",
            "indeedee_female" to "Hembra",
            // Basculegion
            "basculegion_male" to "Macho",
            "basculegion_female" to "Hembra",
            // Oricorio styles
            "baile" to "Apasionado",
            "pom_pom" to "Animado",
            "pau" to "Plácido",
            "sensu" to "Refinado",
            // Silvally / Type Null forms (type-based, hard to map all)
            // Lycanroc
            "midday" to "Diurno",
            "midnight" to "Nocturno",
            "dusk" to "Crepuscular",
            // Toxtricity
            "amped" to "Aguda",
            "low_key" to "Grave",
            // Morpeko
            "full_belly" to "Saciado",
            "hangry" to "Hambriento",
            // Eiscue
            "noice" to "Deshielo",
            // Cramorant
            "gulping" to "Atraganto",
            "gorging" to "Engluto",
            // Mimikyu
            "busted" to " descubierto",
            // Darmanitan
            "zen" to "Zen",
            "galarian_zen" to "Zen Galar",
            // Wishiwashi
            "school" to "Banco",
            // Zygarde forms
            "zygarde_10" to "10%",
            "zygarde_50" to "50%",
            // Furfrou trims
            "heart" to "Corazón",
            "star" to "Estrella",
            "diamond" to "Diamante",
            "debutante" to "Debutante",
            "matron" to "Matrona",
            "dandy" to "Dandi",
            "la_reine" to "La Reine",
            "kabuki" to "Kabuki",
            "pharaoh" to "Faraónico",
            // Deerling/Sawsbuck seasons
            "spring" to "Primavera",
            "summer" to "Verano",
            "autumn" to "Otoño",
            "winter" to "Invierno",
            // Burmy/Wormadam cloaks
            "sandy_cloak" to "Arena",
            "trash_cloak" to "Basura",
            "plant_cloak" to "Planta",
            // Shellos/Gastrodon
            "east" to "Este",
            "west" to "Oeste",
            // Basculin
            "red_striped" to "Raya Roja",
            "blue_striped" to "Raya Azul",
            "white_striped" to "Raya Blanca",
            // Flabébé/Floette/Florges colors
            "red" to "Rojo",
            "blue" to "Azul",
            "yellow" to "Amarillo",
            "orange" to "Naranja",
            "white_flower" to "Blanca",
            // Minior
            "red_core" to "Núcleo Rojo",
            "orange_core" to "Núcleo Naranja",
            "yellow_core" to "Núcleo Amarillo",
            "green_core" to "Núcleo Verde",
            "blue_core" to "Núcleo Azul",
            "indigo_core" to "Núcleo Añil",
            "violet_core" to "Núcleo Violeta",
            // Vivillon patterns
            "archipelago" to "Isleño",
            "continental" to "Continental",
            "elegant" to "Oriental",
            "garden" to "Vergel",
            "high_plains" to "Estepa",
            "icy_snow" to "Polar",
            "jungle" to "Jungla",
            "marine" to "Marino",
            "meadow" to "Floral",
            "modern" to "Moderno",
            "monsoon" to "Monzón",
            "ocean" to "Océano",
            "polar" to "Taiga",
            "river" to "Oasis",
            "sandstorm" to "Desierto",
            "savanna" to "Pantano",
            "sun" to "Solar",
            "tundra" to "Tundra",
            // Unown forms
            "a" to "A", "b" to "B", "c" to "C", "d" to "D", "e" to "E",
            "f" to "F", "g" to "G", "h" to "H", "i" to "I", "j" to "J",
            "k" to "K", "l" to "L", "m" to "M", "n" to "N", "o" to "O",
            "p" to "P", "q" to "Q", "r" to "R", "s" to "S", "t" to "T",
            "u" to "U", "v" to "V", "w" to "W",
            "exclamation" to "!", "question" to "?",
        )
    }
}

data class CounterEntry(
    val speciesId: String,
    val dex: Int,
    val name: String,
    val types: List<PokemonType>,
    val netScore: Double
)

class PokemonTypeProvider {
    @Volatile
    private var allEntries: List<PokemonTypeEntry>? = null
    @Volatile
    private var isLoading = false

    val isLoaded: Boolean get() = allEntries != null

    suspend fun ensureLoaded() {
        if (allEntries != null) return
        if (isLoading) {
            var waited = 0
            while (isLoading && waited < 60) {
                kotlinx.coroutines.delay(500)
                waited++
            }
            return
        }
        isLoading = true
        try {
            val gm = PvPokeApi.fetchGameMaster()
            allEntries = gm.pokemon.map {
                PokemonTypeEntry(
                    speciesId = it.speciesId,
                    dex = it.dex,
                    name = it.speciesName,
                    types = it.types.mapNotNull { t -> PokemonType.fromString(t) }
                )
            }
        } finally {
            isLoading = false
        }
    }

    /** All forms for a given dex number, base form first */
    fun getFormsByDex(dex: Int): List<PokemonTypeEntry> {
        val entries = allEntries ?: return emptyList()
        return entries
            .filter { it.dex == dex }
            .sortedBy { it.speciesId.length } // base form first
    }

    fun getBySpeciesId(speciesId: String): PokemonTypeEntry? =
        allEntries?.find { it.speciesId == speciesId }

    /**
     * Searches all forms by gamemaster name, dex number, or localized base name.
     */
    fun searchAll(
        query: String,
        translator: NameTranslator,
        language: AppLanguage
    ): List<PokemonTypeEntry> {
        val entries = allEntries ?: return emptyList()
        if (query.isBlank()) return entries

        val q = query.trim().lowercase()

        // Build a map of dex → localized base name for matching
        val localizedByDex = translator.getAllNames(language)
            .associate { it.first to it.second.lowercase() }

        return entries.filter { entry ->
            entry.name.lowercase().contains(q) ||
            entry.dex.toString() == q ||
            (localizedByDex[entry.dex]?.contains(q) == true)
        }
    }

    /**
     * Finds the best counter Pokémon for the given target types.
     * A counter is a Pokémon whose types deal super-effective damage to the target
     * while resisting the target's attacks in return.
     */
    fun findCounters(targetTypes: List<PokemonType>, topN: Int = 15): List<CounterEntry> {
        val entries = allEntries ?: return emptyList()
        if (targetTypes.isEmpty()) return emptyList()

        return entries
            .filter { it.types.isNotEmpty() }
            .map { candidate ->
                // Offensive: best multiplier of candidate's types vs target
                val offensive = candidate.types.maxOf { candType ->
                    targetTypes.fold(1.0) { acc, targetType ->
                        acc * TypeChart.getMultiplier(candType, targetType)
                    }
                }

                // Defensive: best multiplier of target's types vs candidate
                val defensive = targetTypes.maxOf { targetType ->
                    candidate.types.fold(1.0) { acc, candType ->
                        acc * TypeChart.getMultiplier(targetType, candType)
                    }
                }

                val netScore = offensive / defensive
                CounterEntry(candidate.speciesId, candidate.dex, candidate.name, candidate.types, netScore)
            }
            .filter { it.netScore > 1.0 }
            .distinctBy { it.dex } // one entry per dex
            .sortedByDescending { it.netScore }
            .take(topN)
    }
}
