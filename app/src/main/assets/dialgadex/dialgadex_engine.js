// DialgaDex PvE engine adapted for Rhino (Android)
// Original: https://github.com/Wujek80/dialgadex

// ---- Settings (overridable from Kotlin) ----
var settings_metric = "eDPS";
var settings_default_level = [40];
var settings_party_size = 1;
var settings_pve_turns = true;
var settings_newdps = true;
var settings_relobbytime = 10;
var settings_team_size_normal = 6;
var settings_team_size_mega = 6;
var settings_xl_budget = false;
var settings_type_affinity = false;
var settings_compare = "top";
var settings_tiermethod = "jenks";
var settings_metric_exp = 0.5;
var estimated_y_numerator = 1340;
var estimated_cm_power = 11670;

// ---- Polyfills ----
if (typeof Math.fround !== 'function') {
    Math.fround = function(x) {
        if (x !== x || x === Infinity || x === -Infinity) return x;
        return java.lang.Float.intBitsToFloat(java.lang.Float.floatToIntBits(x));
    };
}

// ---- Type Chart ----
var POKEMON_TYPES = [
    "Normal","Fire","Water","Grass","Electric","Ice","Fighting","Poison",
    "Ground","Flying","Psychic","Bug","Rock","Ghost","Dragon","Dark","Steel","Fairy"
];

var POKEMON_TYPES_EFFECT = {
    Normal:    [[], ["Ghost"], ["Rock", "Steel"], []],
    Fire:      [[], ["Dragon","Fire","Rock","Water"], ["Bug","Grass","Ice","Steel"]],
    Water:     [[], ["Dragon","Grass","Water"], ["Fire","Ground","Rock"]],
    Grass:     [[], ["Bug","Dragon","Fire","Flying","Grass","Poison","Steel"], ["Ground","Rock","Water"]],
    Electric:  [["Ground"], ["Dragon","Electric","Grass"], ["Flying","Water"]],
    Ice:       [[], ["Fire","Ice","Steel","Water"], ["Dragon","Flying","Grass","Ground"]],
    Fighting:  [["Ghost"], ["Bug","Fairy","Flying","Poison","Psychic"], ["Dark","Ice","Normal","Rock","Steel"]],
    Poison:    [["Steel"], ["Ghost","Ground","Poison","Rock"], ["Fairy","Grass"]],
    Ground:    [["Flying"], ["Bug","Grass"], ["Electric","Fire","Poison","Rock","Steel"]],
    Flying:    [[], ["Electric","Rock","Steel"], ["Bug","Fighting","Grass"]],
    Psychic:   [["Dark"], ["Psychic","Steel"], ["Fighting","Poison"]],
    Bug:       [[], ["Fairy","Fighting","Fire","Flying","Ghost","Poison","Steel"], ["Dark","Grass","Psychic"]],
    Rock:      [[], ["Fighting","Ground","Steel"], ["Bug","Fire","Flying","Ice"]],
    Ghost:     [["Normal"], ["Dark"], ["Ghost","Psychic"]],
    Dragon:    [["Fairy"], ["Steel"], ["Dragon"]],
    Dark:      [[], ["Dark","Fairy","Fighting"], ["Ghost","Psychic"]],
    Steel:     [[], ["Electric","Fire","Steel","Water"], ["Fairy","Ice","Rock"]],
    Fairy:     [[], ["Fire","Poison","Steel"], ["Dark","Dragon","Fighting"]]
};

function GetEffectivenessMultAgainst(attackType, enemyTypes) {
    var effect = POKEMON_TYPES_EFFECT[attackType];
    if (!effect) return 1.0;
    var mult = 1.0;
    for (var ti = 0; ti < enemyTypes.length; ti++) {
        var t = enemyTypes[ti];
        if (effect[0].indexOf(t) !== -1) mult *= 0.390625;
        else if (effect[1].indexOf(t) !== -1) mult *= 0.625;
        else if (effect[2].indexOf(t) !== -1) mult *= Math.fround(1.60);
    }
    return mult;
}

function GetTypesEffectivenessAgainstTypes(types) {
    var result = {};
    for (var ai = 0; ai < POKEMON_TYPES.length; ai++) {
        var at = POKEMON_TYPES[ai];
        result[at] = GetEffectivenessMultAgainst(at, types);
    }
    return result;
}

function GetTypesEffectivenessSingleBoost(type) {
    var boost = Math.fround(1.60);
    var result = {};
    for (var ai = 0; ai < POKEMON_TYPES.length; ai++) {
        var at = POKEMON_TYPES[ai];
        result[at] = (at === type) ? boost : 1.0;
    }
    return result;
}

function GetEffectivenessMultOfType(effectiveness, type) {
    return effectiveness[type] !== undefined ? effectiveness[type] : 1.0;
}

function GetConstantEffectiveness(mult) {
    var result = {};
    for (var ai = 0; ai < POKEMON_TYPES.length; ai++) {
        result[POKEMON_TYPES[ai]] = mult;
    }
    return result;
}

function GetHiddenPowerTypes(filter, pkmTypes) {
    switch (filter) {
        case "None": return [];
        case "Raid Boss": return ["Fighting"];
        case "Type-Match":
            if (pkmTypes) return pkmTypes.filter(function(t) { return t !== "Fairy" && t !== "Normal"; });
            return [];
        default:
            return POKEMON_TYPES.filter(function(t) { return t !== "Fairy" && t !== "Normal"; });
    }
}

// ---- CPM ----
var CPM = [0, 0.094, 0.16639787, 0.21573247, 0.25572005, 0.29024988,
    0.3210876, 0.34921268, 0.3752356, 0.39956728, 0.4225,
    0.44310755, 0.4627984, 0.48168495, 0.49985844, 0.51739395,
    0.5343543, 0.5507927, 0.5667545, 0.5822789, 0.5974,
    0.6121573, 0.6265671, 0.64065295, 0.65443563, 0.667934,
    0.6811649, 0.69414365, 0.7068842, 0.7193991, 0.7317,
    0.7377695, 0.74378943, 0.74976104, 0.7556855, 0.76156384,
    0.76739717, 0.7731865, 0.77893275, 0.784637, 0.7903,
    0.7953, 0.8003, 0.8053, 0.8103, 0.8153, 0.8203, 0.8253,
    0.8303, 0.8353, 0.8403, 0.8453, 0.8503, 0.8553, 0.8603, 0.8653];

var CPM_Map = {};

function GetCPMForLevel(level) {
    if (CPM_Map[level] !== undefined) return CPM_Map[level];
    if (level < 1 || level >= CPM.length) return 0;
    if (Number.isInteger(level) && level >= 1 && level < CPM.length) {
        CPM_Map[level] = Math.fround(CPM[level]);
        return CPM_Map[level];
    }
    var cpmPrev = GetCPMForLevel(Math.floor(level));
    var cpmNext = GetCPMForLevel(Math.ceil(level));
    return Math.sqrt((cpmPrev * cpmPrev + cpmNext * cpmNext) / 2);
}

// ---- Pokemon Forms ----
function GetPokemonForms(pokemon_id) {
    switch (pokemon_id) {
        case 6: return ["Normal","Mega","MegaY"];
        case 359: case 445: case 448: return ["Normal","Mega","MegaZ"];
        case 150: return ["Normal","Mega","MegaY","A"];
        case 3: case 9: case 15: case 18: case 36: case 65: case 71: case 94:
        case 115: case 121: case 127: case 130: case 142: case 149: case 154:
        case 160: case 181: case 208: case 212: case 214: case 229: case 248:
        case 254: case 257: case 260: case 277: case 282: case 302: case 303:
        case 306: case 308: case 310: case 319: case 323: case 334: case 354:
        case 358: case 362: case 373: case 376: case 380: case 381: case 382:
        case 383: case 384: case 398: case 428: case 460: case 475: case 478:
        case 485: case 491: case 500: case 530: case 531: case 545: case 560:
        case 604: case 609: case 623: case 652: case 655: case 658: case 687:
        case 689: case 691: case 701: case 719: case 740: case 768: case 780:
        case 801: case 807: case 870: case 952: case 970: case 998:
            return ["Normal","Mega"];
        case 80: return ["Normal","Galarian","Mega"];
        case 26: return ["Normal","Alola","Mega","MegaY"];
        case 668: case 678: return ["Normal","Female","Mega"];
        case 670: return ["Red","Yellow","Orange","Blue","White","Mega"];
        case 718: return ["Fifty_percent","Ten_percent","Complete","Mega"];
        case 19: case 20: case 27: case 28: case 37: case 38: case 50: case 51:
        case 53: case 74: case 75: case 76: case 88: case 89: case 103: case 105:
            return ["Normal","Alola"];
        case 77: case 78: case 79: case 83: case 110: case 122: case 144:
        case 145: case 146: case 199: case 222: case 263: case 264: case 554:
        case 562: case 618:
            return ["Normal","Galarian"];
        case 52: return ["Normal","Alola","Galarian"];
        case 58: case 59: case 100: case 101: case 157: case 211: case 215:
        case 503: case 549: case 570: case 571: case 628: case 705: case 706:
        case 713: case 724:
            return ["Normal","Hisuian"];
        case 194: return ["Normal","Paldea"];
        case 128: return ["Normal","Paldea_combat","Paldea_aqua","Paldea_blaze"];
        case 25: return ["Normal","Flying_01","Doctor","Horizons","Pop_star","Rock_star","Vs_2019"];
        case 201: return ["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Exclamation_point","Question_mark"];
        case 249: case 250: return ["Normal","S"];
        case 351: return ["Normal","Sunny","Rainy","Snowy"];
        case 386: return ["Normal","Attack","Defense","Speed"];
        case 412: case 413: return ["Plant","Sandy","Trash"];
        case 421: return ["Overcast","Sunny"];
        case 422: case 423: return ["West_sea","East_sea"];
        case 479: return ["Normal","Heat","Wash","Frost","Fan","Mow"];
        case 483: case 484: return ["Normal","Origin"];
        case 487: return ["Altered","Origin"];
        case 492: return ["Land","Sky"];
        case 493: case 773:
            return ["Normal","Fire","Water","Grass","Electric","Ice","Fighting","Poison","Ground","Flying","Psychic","Bug","Rock","Ghost","Dragon","Dark","Steel","Fairy"];
        case 550: return ["Red_striped","Blue_striped","White_striped"];
        case 555: return ["Standard","Zen","Galarian_standard","Galarian_zen"];
        case 585: case 586: return ["Spring","Summer","Autumn","Winter"];
        case 592: case 593: return ["Normal","Female"];
        case 641: case 642: case 645: case 905: return ["Incarnate","Therian"];
        case 646: return ["Normal","White","Black"];
        case 647: return ["Ordinary","Resolute"];
        case 648: return ["Aria","Pirouette"];
        case 649: return ["Normal","Shock","Burn","Chill","Douse"];
        case 666: return ["Meadow","Archipelago","Continental","Elegant","Fancy","Garden","High_plains","Icy_snow","Jungle","Marine","Modern","Monsoon","Ocean","Poke_ball","Polar","River","Sandstorm","Savanna","Sun","Tundra"];
        case 669: case 671: return ["Red","Yellow","Orange","Blue","White"];
        case 676: return ["Natural","Heart","Star","Diamond","Debutante","Matron","Dandy","La_reine","Kabuki","Pharaoh"];
        case 681: return ["Normal","Blade"];
        case 710: case 711: return ["Average","Small","Large","Super"];
        case 720: return ["Confined","Unbound"];
        case 741: return ["Baile","Pompom","Pau","Sensu"];
        case 745: return ["Midday","Midnight","Dusk"];
        case 746: return ["Solo","School"];
        case 778: return ["Disguised","Busted"];
        case 800: return ["Normal","Dawn_wings","Dusk_mane","Ultra"];
        case 849: return ["Amped","Low_key"];
        case 854: case 855: return ["Phony","Antique"];
        case 875: return ["Ice","Noice"];
        case 876: return ["Male","Female"];
        case 877: return ["Full_belly","Hangry"];
        case 888: return ["Hero","Crowned_sword"];
        case 889: return ["Hero","Crowned_shield"];
        case 890: return ["Normal","Eternamax"];
        case 892: return ["Single_strike","Rapid_strike"];
        case 898: return ["Normal","Ice_rider","Shadow_rider"];
        case 902: return ["Normal","Female"];
        case 916: return ["Normal","Female"];
        case 925: return ["Family_of_four","Family_of_three"];
        case 964: return ["Zero","Hero"];
        case 978: return ["Curly","Droopy","Stretchy","Mega"];
        case 982: return ["Two","Three"];
        case 1012: case 1013: return ["Counterfeit","Artisan"];
        default: return ["Normal"];
    }
}

// ---- Utility Functions ----
function GetPokemonStats(pkm_obj, level, ivs) {
    if (!level) level = settings_default_level[0];
    if (!ivs) ivs = {atk: 15, def: 15, hp: 15};
    var stats = pkm_obj.stats;
    var cpm = GetCPMForLevel(level);
    return {
        atk: (stats.baseAttack + ivs.atk) * cpm,
        def: (stats.baseDefense + ivs.def) * cpm,
        hp: (stats.baseStamina + ivs.hp) * cpm
    };
}

function GetPokemonMoves(pkm_obj, hidden_power_filter) {
    if (!pkm_obj.fm && !pkm_obj.cm) return [];
    if (!hidden_power_filter) hidden_power_filter = "Type-Match";
    var fm = pkm_obj.fm ? pkm_obj.fm.slice() : [];
    var elite_fm = pkm_obj.elite_fm ? pkm_obj.elite_fm.slice() : [];
    var cm = pkm_obj.cm ? pkm_obj.cm.slice() : [];
    var elite_cm = pkm_obj.elite_cm ? pkm_obj.elite_cm.slice() : [];

    if (fm.indexOf("Hidden Power") !== -1 || elite_fm.indexOf("Hidden Power") !== -1) {
        var hpTypes = GetHiddenPowerTypes(hidden_power_filter, pkm_obj.types);
        for (var hi = 0; hi < hpTypes.length; hi++) {
            if (fm.indexOf("Hidden Power") !== -1) fm.push("Hidden Power " + hpTypes[hi]);
            if (elite_fm.indexOf("Hidden Power") !== -1) elite_fm.push("Hidden Power " + hpTypes[hi]);
        }
    }

    var shadow_only_cm = [];
    var pure_only_cm = [];
    if (pkm_obj.shadow) {
        pure_only_cm.push("Return");
    }

    if (pkm_obj.fm_add) {
        for (var fi = 0; fi < pkm_obj.fm_add.length; fi++) elite_fm.push(pkm_obj.fm_add[fi]);
    }
    if (pkm_obj.cm_add) {
        for (var ci = 0; ci < pkm_obj.cm_add.length; ci++) elite_cm.push(pkm_obj.cm_add[ci]);
    }
    if (pkm_obj.fm_rem) {
        var fmRemSet = {};
        for (var ri = 0; ri < pkm_obj.fm_rem.length; ri++) fmRemSet[pkm_obj.fm_rem[ri]] = true;
        fm = fm.filter(function(f) { return !fmRemSet[f]; });
        elite_fm = elite_fm.filter(function(f) { return !fmRemSet[f]; });
    }
    if (pkm_obj.cm_rem) {
        var cmRemSet = {};
        for (var ri = 0; ri < pkm_obj.cm_rem.length; ri++) cmRemSet[pkm_obj.cm_rem[ri]] = true;
        cm = cm.filter(function(c) { return !cmRemSet[c]; });
        elite_cm = elite_cm.filter(function(c) { return !cmRemSet[c]; });
    }

    return [fm, cm, elite_fm, elite_cm, pure_only_cm, shadow_only_cm];
}

function GetUniqueIdentifier(pkm_obj, unique_shadow, unique_level, unique_moves, elite_moves_only) {
    if (unique_shadow === undefined) unique_shadow = true;
    if (unique_level === undefined) unique_level = false;
    if (unique_moves === undefined) unique_moves = false;
    if (elite_moves_only === undefined) elite_moves_only = false;
    return pkm_obj.id + "-" + pkm_obj.form +
        (unique_shadow ? "-" + pkm_obj.shadow : "") +
        (unique_level ? "-" + (pkm_obj.level !== undefined ? pkm_obj.level : settings_default_level[0]) : "") +
        (unique_moves ? "-" + (pkm_obj.fm_is_elite || !elite_moves_only ? pkm_obj.fm : "null") +
            "-" + (pkm_obj.cm_is_elite || !elite_moves_only ? pkm_obj.cm : "null") : "");
}

// ---- Calc Functions ----
function ProcessDuration(duration) {
    if (settings_pve_turns) return Math.round((duration / 1000) * 2) / 2;
    return duration / 1000;
}

function ProcessPower(move_obj) {
    if (settings_pve_turns) {
        var newDuration = ProcessDuration(move_obj.duration);
        var modifier = (newDuration - move_obj.duration / 1000) / newDuration;
        if (Math.abs(modifier) >= 0.199) return move_obj.power * (1 + modifier);
    }
    return move_obj.power;
}

function CalcDamage(atk, def, power, modifiers, rounded) {
    if (rounded) return Math.floor(Math.fround(0.5 * power * (atk / def) * modifiers)) + 1;
    return 0.5 * power * (atk / def) * modifiers + 0.5;
}

function GetPartyBoost(f_to_c_ratio) {
    if (settings_party_size === 1) return 0;
    var f_moves_per_boost;
    switch (settings_party_size) {
        case 2: f_moves_per_boost = 18; break;
        case 3: f_moves_per_boost = 9; break;
        case 4: f_moves_per_boost = 6; break;
        default: return 0;
    }
    return Math.max(0, Math.min(f_to_c_ratio / f_moves_per_boost, 1));
}

function GetDPS(types, atk, def, hp, fm_obj, cm_obj, fm_mult, cm_mult, enemy_def, enemy_y, real_damage) {
    if (!fm_obj || !cm_obj) return 0;
    if (!enemy_y) enemy_y = {y_num: null, cm_num: null};
    if (!enemy_def) enemy_def = 180;
    var y = (enemy_y.y_num ? enemy_y.y_num : estimated_y_numerator) / def;
    var in_cm_dmg = (enemy_y.cm_num ? enemy_y.cm_num : estimated_cm_power) / def;

    var tof = hp / y;
    var x = 0.5 * -cm_obj.energy_delta + 0.5 * fm_obj.energy_delta;
    if (settings_newdps) x = x + 0.5 * in_cm_dmg;

    var fm_dmg_mult = fm_mult * ((types.indexOf(fm_obj.type) !== -1 && fm_obj.name !== "Hidden Power") ? Math.fround(1.2) : 1);
    var fm_dmg = CalcDamage(atk, enemy_def, ProcessPower(fm_obj), fm_dmg_mult, real_damage);
    var fm_dps = fm_dmg / ProcessDuration(fm_obj.duration);
    var fm_eps = fm_obj.energy_delta / ProcessDuration(fm_obj.duration);

    var f_to_c_ratio = (tof * -cm_obj.energy_delta + ProcessDuration(cm_obj.duration) * (x - 0.5 * hp)) /
        (tof * fm_obj.energy_delta - ProcessDuration(fm_obj.duration) * (x - 0.5 * hp));
    var pp_boost = GetPartyBoost(f_to_c_ratio);

    var cm_dmg_mult = cm_mult * ((types.indexOf(cm_obj.type) !== -1) ? Math.fround(1.2) : 1);
    var cm_dmg = CalcDamage(atk, enemy_def, ProcessPower(cm_obj), cm_dmg_mult, real_damage);
    var cm_dps = cm_dmg / ProcessDuration(cm_obj.duration);
    var cm_dps_adj = cm_dps * (1 + pp_boost);
    var cm_eps = -cm_obj.energy_delta / ProcessDuration(cm_obj.duration);
    if (cm_obj.energy_delta === -100) {
        var dws = (settings_pve_turns ? 0 : cm_obj.damage_window_start / 1000);
        cm_eps = (-cm_obj.energy_delta + 0.5 * fm_obj.energy_delta + 0.5 * y * dws) / ProcessDuration(cm_obj.duration);
    }

    if (fm_dps > cm_dps) return fm_dps;

    var dps0 = (fm_dps * cm_eps + cm_dps_adj * fm_eps) / (cm_eps + fm_eps);
    var dps = dps0 + ((cm_dps_adj - fm_dps) / (cm_eps + fm_eps)) * (0.5 - x / hp) * y;
    return (fm_dps > dps ? fm_dps : (dps > 0 ? dps : 0));
}

function GetTDO(dps, hp, def, enemy_y) {
    var y = (enemy_y && enemy_y.y_num ? enemy_y.y_num : estimated_y_numerator) / def;
    var tof = hp / y;
    return dps * tof;
}

function GetEDPS(dps, tdo, pkm_obj, enemy_params) {
    var RESPAWN_TIME = 1;
    var REJOIN_TIME = settings_relobbytime;
    var RAID_PARTY_SIZE = (pkm_obj && (pkm_obj.form === "Mega" || pkm_obj.form === "MegaY" || pkm_obj.form === "MegaZ"))
        ? settings_team_size_mega : settings_team_size_normal;
    var hp = (enemy_params && enemy_params.stats && enemy_params.stats.hp) ? enemy_params.stats.hp : 1000000000;
    var tof = tdo / dps;
    var lives = hp / tdo;
    var deaths = Math.ceil(lives) - 1;
    var relobbies = Math.floor(deaths / RAID_PARTY_SIZE);
    var ttw = lives * tof + (deaths - relobbies) * RESPAWN_TIME + REJOIN_TIME * relobbies;
    return hp / ttw;
}

function GetMetric(dps, tdo, pkm_obj, enemy_params) {
    if (settings_metric === "eDPS") return GetEDPS(dps, tdo, pkm_obj, enemy_params);
    return Math.pow(dps, 1 - settings_metric_exp) * Math.pow(tdo, settings_metric_exp);
}

function AvgYAgainst(enemy_y, effectiveness) {
    var y = {y_num: 0, cm_num: 0};
    var keys = Object.keys(enemy_y);
    for (var ki = 0; ki < keys.length; ki++) {
        var t = keys[ki];
        if (t === "Any") continue;
        var mult = GetEffectivenessMultOfType(effectiveness, t);
        if (isNaN(mult)) mult = 1;
        y.y_num += enemy_y[t].y_num * mult;
        y.cm_num += enemy_y[t].cm_num * mult;
    }
    return y;
}

function GetSpecificY(types, atk, fm_obj, cm_obj, total_incoming_dps) {
    if (!fm_obj || !cm_obj) return 0;
    if (total_incoming_dps === undefined) total_incoming_dps = 50;
    var CHARGED_MOVE_CHANCE = 0.3;
    var ENERGY_PER_HP = 0.5;
    var FM_DELAY = 1.75;
    var CM_DELAY = 0.5;
    var fm_stab = (types.indexOf(fm_obj.type) !== -1 && fm_obj.name !== "Hidden Power") ? Math.fround(1.2) : 1;
    var fm_num = 0.5 * ProcessPower(fm_obj) * fm_stab * atk;
    var fm_dur = ProcessDuration(fm_obj.duration);
    var cm_stab = (types.indexOf(cm_obj.type) !== -1) ? Math.fround(1.2) : 1;
    var cm_num = 0.5 * ProcessPower(cm_obj) * cm_stab * atk;
    var cm_dur = ProcessDuration(cm_obj.duration);
    var fms_per_cm = 1;
    if (settings_newdps) {
        var eps_for_damage = ENERGY_PER_HP * total_incoming_dps;
        fm_dur = fm_dur + FM_DELAY;
        cm_dur = cm_dur + CM_DELAY;
        fms_per_cm = (-cm_obj.energy_delta - eps_for_damage * cm_dur) / (fm_obj.energy_delta + eps_for_damage * fm_dur);
        if (fms_per_cm < 0) fms_per_cm = 0;
        fms_per_cm = fms_per_cm + (1 / CHARGED_MOVE_CHANCE) - 1;
    } else {
        switch (cm_obj.energy_delta) {
            case -100: fms_per_cm = 3; break;
            case -50: fms_per_cm = 1.5; break;
            case -33: fms_per_cm = 1; break;
        }
        fms_per_cm = fms_per_cm * 0.5;
        fm_dur += 2;
        cm_dur += 2;
    }
    var cycle_dur = fms_per_cm * fm_dur + cm_dur;
    var type_ys = {"Any": {y_num: (fms_per_cm * fm_num + cm_num) / cycle_dur, cm_num: cm_num}};
    if (fm_obj.type === cm_obj.type) {
        type_ys[fm_obj.type] = type_ys["Any"];
    } else {
        type_ys[fm_obj.type] = {y_num: (fms_per_cm * fm_num) / cycle_dur, cm_num: 0};
        type_ys[cm_obj.type] = {y_num: cm_num / cycle_dur, cm_num: cm_num};
    }
    return type_ys;
}

function GetMovesetYs(types, atk, fms, cms, total_incoming_dps) {
    var all_ys = [];
    for (var fi = 0; fi < fms.length; fi++) {
        var fm_name = fms[fi];
        var fm_obj = null;
        for (var mi = 0; mi < jb_fm.length; mi++) {
            if (jb_fm[mi].name === fm_name) { fm_obj = jb_fm[mi]; break; }
        }
        if (!fm_obj) continue;
        for (var ci = 0; ci < cms.length; ci++) {
            var cm_name = cms[ci];
            var cm_obj = null;
            for (var mi = 0; mi < jb_cm.length; mi++) {
                if (jb_cm[mi].name === cm_name) { cm_obj = jb_cm[mi]; break; }
            }
            if (!cm_obj) continue;
            all_ys.push(GetSpecificY(types, atk, fm_obj, cm_obj, total_incoming_dps));
        }
    }
    return all_ys;
}

function GetAvgY(all_ys) {
    if (all_ys.length === 0) return {};
    var avg_ys = {};
    for (var yi = 0; yi < all_ys.length; yi++) {
        var this_y = all_ys[yi];
        var keys = Object.keys(this_y);
        for (var ki = 0; ki < keys.length; ki++) {
            var type = keys[ki];
            if (!avg_ys[type]) avg_ys[type] = {y_num: 0, cm_num: 0};
            avg_ys[type].y_num += this_y[type].y_num;
            avg_ys[type].cm_num += this_y[type].cm_num;
        }
    }
    var typeKeys = Object.keys(avg_ys);
    for (var ti = 0; ti < typeKeys.length; ti++) {
        avg_ys[typeKeys[ti]].y_num /= all_ys.length;
        avg_ys[typeKeys[ti]].cm_num /= all_ys.length;
    }
    return avg_ys;
}

function GetRaidStats(pkm_obj, tier) {
    if (!tier) {
        if (pkm_obj.raid_tier) tier = pkm_obj.raid_tier;
        else {
            tier = 3;
            if (pkm_obj.class) tier = 5;
            if (pkm_obj.form === "Mega" || pkm_obj.form === "MegaY" || pkm_obj.form === "MegaZ") tier = 4;
            if (pkm_obj.class && pkm_obj.form === "Mega") tier = 6;
        }
    }
    var ivs = {atk: 15, def: 15, hp: 15};
    var cpm = [0, 0.6, 0.67, 0.73, 0.79, 0.79, 0.79, 1.0, 0.79][tier];
    var stats = {baseAttack: pkm_obj.stats.baseAttack, baseDefense: pkm_obj.stats.baseDefense, baseStamina: pkm_obj.stats.baseStamina};
    return {
        atk: (stats.baseAttack + ivs.atk) * (Math.fround ? Math.fround(cpm) : cpm),
        def: (stats.baseDefense + ivs.def) * (Math.fround ? Math.fround(cpm) : cpm),
        hp: [0, 600, 0, 3600, 9000, 15000, 22500, 20000, 25000][tier]
    };
}

function GetRaidBosses(has_type, weak_to_type) {
    var raid_bosses = [];
    for (var pi = 0; pi < jb_pkm.length; pi++) {
        var pkm_obj = jb_pkm[pi];
        if (!pkm_obj.raid_tier || pkm_obj.raid_tier < 4) continue;
        if (has_type && pkm_obj.types.indexOf(has_type) === -1) continue;
        if (weak_to_type && GetEffectivenessMultAgainst(weak_to_type, pkm_obj.types) <= 1.01) continue;
        raid_bosses.push(pkm_obj);
    }
    return raid_bosses;
}

function GetTypeAffinity(type, versus) {
    var raid_bosses;
    if (type === "Any") raid_bosses = GetRaidBosses();
    else if (versus) raid_bosses = GetRaidBosses(null, type);
    else raid_bosses = GetRaidBosses(type, null);
    if (raid_bosses.length === 0) raid_bosses = GetRaidBosses();

    var avg_effectiveness = {};
    var avg_ys = {};
    for (var ti = 0; ti < POKEMON_TYPES.length; ti++) {
        avg_effectiveness[POKEMON_TYPES[ti]] = 0;
        avg_ys[POKEMON_TYPES[ti]] = {y_num: 0, cm_num: 0};
    }
    avg_ys["Any"] = {y_num: 0, cm_num: 0};
    avg_ys["None"] = {y_num: 0, cm_num: 0};
    var avg_stats = {atk: 0, def: 0, hp: 0};

    for (var bi = 0; bi < raid_bosses.length; bi++) {
        var boss = raid_bosses[bi];
        var effectiveness = GetTypesEffectivenessAgainstTypes(boss.types);
        for (var ti = 0; ti < POKEMON_TYPES.length; ti++) {
            avg_effectiveness[POKEMON_TYPES[ti]] += GetEffectivenessMultOfType(effectiveness, POKEMON_TYPES[ti]);
        }
        var stats = GetRaidStats(boss);
        avg_stats.atk += stats.atk;
        avg_stats.def += stats.def;
        avg_stats.hp += stats.hp;
        var moves = GetPokemonMoves(boss, "Raid Boss");
        if (moves.length === 6) {
            var win_dps = stats.hp / (boss.raid_tier >= 4 ? 300 : 180);
            var boss_ys = GetMovesetYs(boss.types, stats.atk, moves[0], moves[1], win_dps);
            var boss_avg_y = GetAvgY(boss_ys);
            var yKeys = Object.keys(boss_avg_y);
            for (var yi = 0; yi < yKeys.length; yi++) {
                var t = yKeys[yi];
                avg_ys[t].y_num += boss_avg_y[t].y_num;
                avg_ys[t].cm_num += boss_avg_y[t].cm_num;
            }
        }
    }

    for (var ti = 0; ti < POKEMON_TYPES.length; ti++) {
        avg_effectiveness[POKEMON_TYPES[ti]] /= raid_bosses.length;
    }
    var yKeys = Object.keys(avg_ys);
    for (var yi = 0; yi < yKeys.length; yi++) {
        avg_ys[yKeys[yi]].y_num /= raid_bosses.length;
        avg_ys[yKeys[yi]].cm_num /= raid_bosses.length;
    }
    avg_stats.atk /= raid_bosses.length;
    avg_stats.def /= raid_bosses.length;
    avg_stats.hp /= raid_bosses.length;

    return {weakness: avg_effectiveness, enemy_ys: [avg_ys], stats: avg_stats};
}

// ---- Core Ranking Function ----
function GetStrongestAgainstSpecificEnemy(pkm_obj, shadow, level, enemy_params, search_params) {
    var num_movesets = search_params.suboptimal ? 50 : 1;
    var movesets = [];
    var types = pkm_obj.types;
    var effectiveness = GetTypesEffectivenessAgainstTypes(types);
    var stats = GetPokemonStats(pkm_obj, level);
    stats.hp = Math.floor(stats.hp);
    var atk = shadow ? (stats.atk * Math.fround(1.2)) : stats.atk;
    var def = shadow ? (stats.def * Math.fround(0.8333333)) : stats.def;
    var hp = stats.hp;

    var moves = GetPokemonMoves(pkm_obj, "All");
    if (moves.length !== 6) return movesets;
    var fms = moves[0];
    var cms = moves[1];
    var elite_fms = moves[2];
    var elite_cms = moves[3];
    var pure_only_cms = moves[4];
    var shadow_only_cms = moves[5];
    if (shadow === true) elite_cms = elite_cms.concat(shadow_only_cms);
    else if (shadow === false) elite_cms = elite_cms.concat(pure_only_cms);
    var all_fms = fms.concat(elite_fms);
    var all_cms = cms.concat(elite_cms);

    var enemy_moveset_ys = enemy_params.enemy_ys;
    var enemy_effectiveness = enemy_params.weakness;
    var enemy_stats = enemy_params.stats;
    var enemy_def = enemy_stats ? enemy_stats.def : null;
    var enemy_moves = enemy_params.moves;

    if ((!enemy_moveset_ys || enemy_moveset_ys.length === 0) && enemy_moves && enemy_moves.length === 6 && enemy_stats) {
        var enemy_fms = enemy_moves[0];
        var enemy_cms = enemy_moves[1];
        var enemy_all_fms = enemy_fms.concat([]);
        var enemy_all_cms = enemy_cms.concat([]);
        var incoming_dps = enemy_params.win_dps || 50;
        enemy_moveset_ys = GetMovesetYs(enemy_types, enemy_stats.atk, enemy_all_fms, enemy_all_cms, incoming_dps);
    }

    for (var fi = 0; fi < all_fms.length; fi++) {
        var fm = all_fms[fi];
        var fm_is_elite = elite_fms.indexOf(fm) !== -1;
        if (!search_params.elite && fm_is_elite) continue;

        var fm_obj = null;
        for (var mi = 0; mi < jb_fm.length; mi++) {
            if (jb_fm[mi].name === fm) { fm_obj = jb_fm[mi]; break; }
        }
        if (!fm_obj) continue;

        if (search_params.type && search_params.type !== "Any" && !search_params.versus &&
            fm_obj.type !== search_params.type && !search_params.mixed) continue;

        var fm_mult = GetEffectivenessMultOfType(enemy_effectiveness, fm_obj.type);

        for (var ci = 0; ci < all_cms.length; ci++) {
            var cm = all_cms[ci];
            var cm_is_elite = elite_cms.indexOf(cm) !== -1;
            if (!search_params.elite && cm_is_elite) continue;

            var cm_obj = null;
            for (var mi = 0; mi < jb_cm.length; mi++) {
                if (jb_cm[mi].name === cm) { cm_obj = jb_cm[mi]; break; }
            }
            if (!cm_obj) continue;

            if (search_params.type && search_params.type !== "Any" && !search_params.versus &&
                cm_obj.type !== search_params.type && !search_params.mixed) continue;

            if (search_params.type && search_params.type !== "Any" && !search_params.versus && !search_params.offtype &&
                search_params.mixed && fm_obj.type !== search_params.type && cm_obj.type !== search_params.type) continue;

            if (fm_obj.type !== cm_obj.type && !search_params.mixed) continue;

            var cm_mult = GetEffectivenessMultOfType(enemy_effectiveness, cm_obj.type);

            var sumRat = 0, sumDps = 0, sumTdo = 0;
            for (var ei = 0; ei < enemy_moveset_ys.length; ei++) {
                var y = AvgYAgainst(enemy_moveset_ys[ei], effectiveness);
                var dps = GetDPS(types, atk, def, hp, fm_obj, cm_obj, fm_mult, cm_mult, enemy_def, y, search_params.real_damage);
                var tdo = GetTDO(dps, hp, def, y);
                var rat = GetMetric(dps, tdo, pkm_obj, enemy_params);
                sumRat += rat;
                sumDps += dps;
                sumTdo += tdo;
            }
            if (enemy_moveset_ys.length > 0) {
                sumRat /= enemy_moveset_ys.length;
                sumDps /= enemy_moveset_ys.length;
                sumTdo /= enemy_moveset_ys.length;
            }

            if (movesets.length < num_movesets) {
                movesets.push({
                    rat: sumRat, dps: sumDps, tdo: sumTdo,
                    fm: fm, fm_is_elite: fm_is_elite, fm_type: fm_obj.type,
                    cm: cm, cm_is_elite: cm_is_elite, cm_type: cm_obj.type
                });
            } else if (sumRat > movesets[0].rat) {
                movesets[0] = {
                    rat: sumRat, dps: sumDps, tdo: sumTdo,
                    fm: fm, fm_is_elite: fm_is_elite, fm_type: fm_obj.type,
                    cm: cm, cm_is_elite: cm_is_elite, cm_type: cm_obj.type
                };
                movesets.sort(function(a, b) { return a.rat > b.rat ? 1 : (a.rat < b.rat ? -1 : 0); });
            }
        }
    }

    movesets.sort(function(a, b) { return a.rat > b.rat ? 1 : (a.rat < b.rat ? -1 : 0); });
    return movesets;
}

// ---- Wrapper that iterates ALL Pokemon like SearchAll (by ID + GetPokemonForms) ----
function computeAll(pkmJson, fmJson, cmJson, count, includeShadow, includeMega, includeLegendary, includeUnreleased, mixed) {
    var pkmList = JSON.parse(pkmJson);
    var fmList = JSON.parse(fmJson);
    var cmList = JSON.parse(cmJson);

    jb_pkm = pkmList;
    jb_fm = fmList;
    jb_cm = cmList;

    var jb_max_id = 0;
    for (var pi = 0; pi < pkmList.length; pi++) {
        if (pkmList[pi].id > jb_max_id) jb_max_id = pkmList[pi].id;
    }

    var affinity = GetTypeAffinity("Any", false);
    var enemy_params = {
        weakness: affinity.weakness,
        enemy_ys: affinity.enemy_ys,
        stats: affinity.stats
    };

    var search_params = {
        type: "Any",
        mixed: mixed,
        elite: true,
        offtype: true,
        suboptimal: false,
        shadow: includeShadow,
        mega: includeMega,
        legendary: includeLegendary,
        unreleased: includeUnreleased,
        real_damage: false,
        versus: false
    };

    var results = [];
    var seenKeys = {};

    function isCostumeForm(form) {
        var costumeIndicators = ["_2019","_2020","_2021","_2022","_2023","_2024","_2025","_2026","Copy_","Fall_","Costume_","Adventure_hat","Flying_","Summer_","Winter_","Spring_","Gofest_","Gotour_","Tshirt_","Holiday_","Swim_"];
        for (var ii = 0; ii < costumeIndicators.length; ii++) {
            if (form.indexOf(costumeIndicators[ii]) !== -1) return true;
        }
        if (form.split("").every(function(c) { return c >= '0' && c <= '9'; })) return true;
        return false;
    }

    function findPkm(id, form) {
        for (var pi = 0; pi < pkmList.length; pi++) {
            if (pkmList[pi].id === id && pkmList[pi].form === form) return pkmList[pi];
        }
        return null;
    }

    function processOne(pkm, shadow) {
        if (!pkm) return;
        var key = pkm.id + "-" + pkm.form + "-" + shadow;
        if (seenKeys[key]) return;
        seenKeys[key] = true;

        var movesets = GetStrongestAgainstSpecificEnemy(pkm, shadow, 40, enemy_params, search_params);
        if (movesets.length === 0) return;

        var best = movesets[movesets.length - 1];
        var isMega = pkm.form === "Mega" || pkm.form === "MegaY" || pkm.form === "MegaZ";

        results.push({
            rat: best.rat, dps: best.dps, tdo: best.tdo,
            id: pkm.id, name: pkm.name, form: pkm.form,
            shadow: shadow, level: 40,
            fm: best.fm, fmIsElite: best.fm_is_elite, fmType: best.fm_type,
            cm: best.cm, cmIsElite: best.cm_is_elite, cmType: best.cm_type,
            tier: isMega ? "Mega" : null
        });
    }

    for (var id = 1; id <= jb_max_id; id++) {
        var forms = GetPokemonForms(id);
        var def_form = forms[0];
        var pkm = findPkm(id, def_form);

        if (!pkm || (!includeUnreleased && !pkm.released)
            || (!includeLegendary && pkm.class !== undefined)) {
            continue;
        }

        processOne(pkm, false);
        if (includeShadow && pkm.shadow) processOne(pkm, true);

        for (var fi = 1; fi < forms.length; fi++) {
            var formName = forms[fi];
            pkm = findPkm(id, formName);
            if (!pkm || (!includeUnreleased && !pkm.released)
                || (!includeMega && (formName === "Mega" || formName === "MegaY" || formName === "MegaZ"))) {
                continue;
            }
            processOne(pkm, false);
            if (includeShadow && pkm.shadow) processOne(pkm, true);
        }
    }

    results.sort(function(a, b) { return b.rat - a.rat; });
    results = results.slice(0, count);
    return JSON.stringify(results);
}
