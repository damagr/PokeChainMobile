package com.pokechain.data.scanner

import com.pokechain.data.models.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Resultado parcial del escáner: cada frame detectado actualiza los campos
 * que puede leer (PC en la pantalla principal, barras y PS en la Valoración,
 * peso y altura en la página del Pokémon).
 * Los campos null se conservan del estado anterior al hacer merge.
 */
data class ScanResult(
    val speciesName: String? = null,
    val dex: Int? = null,
    val cp: Int? = null,
    val hp: Int? = null,
    val heightM: Double? = null,
    val weightKg: Double? = null,
    val ivAtk: Int? = null,
    val ivDef: Int? = null,
    val ivSta: Int? = null,
    /** Solo en modo SHOWCASE: puntuación total calculada (de XXXX/1178). */
    val showcaseTotal: Int? = null,
) {
    /** % de perfección: solo con las tres barras leídas (IVs exactos). */
    val perfection: Double?
        get() = if (ivAtk != null && ivDef != null && ivSta != null)
            (ivAtk + ivDef + ivSta) / 45.0 * 100.0 else null

    /**
     * Acumula un frame nuevo. Si cambia la especie se considera un Pokémon
     * distinto y se reinicia el estado (evita mezclar datos de dos Pokémon).
     */
    fun mergeWith(new: ScanResult): ScanResult {
        val speciesChanged = new.dex != null && dex != null && new.dex != dex
        val base = if (speciesChanged) ScanResult() else this
        return base.copy(
            speciesName = new.speciesName ?: base.speciesName,
            dex = new.dex ?: base.dex,
            cp = new.cp ?: base.cp,
            hp = new.hp ?: base.hp,
            heightM = new.heightM ?: base.heightM,
            weightKg = new.weightKg ?: base.weightKg,
            ivAtk = new.ivAtk ?: base.ivAtk,
            ivDef = new.ivDef ?: base.ivDef,
            ivSta = new.ivSta ?: base.ivSta,
            showcaseTotal = new.showcaseTotal ?: base.showcaseTotal,
        )
    }
}

/** Qué calculadora alimenta el escáner: define qué muestra el chip flotante. */
enum class ScannerMode { IV, SHOWCASE }

/**
 * Etapas del flujo de escaneo:
 *  - LOOKING_POKEMON: buscando la página del Pokémon (nombre/peso/altura/PC)
 *  - POKEMON_READ: datos del Pokémon confirmados — pendiente la Valoración
 *  - IV_READ: barras confirmadas — COMPLETO (solo vigilancia de cambios)
 */
enum class ScanStage { LOOKING_POKEMON, POKEMON_READ, IV_READ }

/** Estado compartido entre el servicio de escaneo y la UI (Calc. IV / Exhibición). */
object ScannerState {
    val isRunning = MutableStateFlow(false)
    val lastResult = MutableStateFlow<ScanResult?>(null)

    /** Etapa actual del flujo. */
    val stage = MutableStateFlow(ScanStage.LOOKING_POKEMON)

    /** El frame actual pertenece a la pantalla que espera la etapa (feedback "Leyendo…"). */
    val reading = MutableStateFlow(false)

    /** Idioma de la app en el momento de iniciar el escaneo (para el overlay). */
    @Volatile
    var language: AppLanguage = AppLanguage.ES

    /** Modo del escaneo: IV (por defecto) o SHOWCASE. Se fija al iniciar. */
    @Volatile
    var mode: ScannerMode = ScannerMode.IV

    /** Posición arrastrada del chip (sesión). */
    @Volatile
    var chipX: Int = 60

    @Volatile
    var chipY: Int = 140

    /** PokeChain en primer plano -> el chip se oculta (solo aparece sobre PoGo). */
    val appInForeground = MutableStateFlow(true)

    /** El usuario ha pulsado iniciar en el chip: el servicio procesa frames. */
    val scanningEnabled = MutableStateFlow(false)

    fun reset() {
        lastResult.value = null
        stage.value = ScanStage.LOOKING_POKEMON
        reading.value = false
        scanningEnabled.value = false
    }

    /** Reinicio del flujo desde el chip (botón ↺): vuelve a buscar un Pokémon. */
    fun resetFlow() {
        reset()
    }
}