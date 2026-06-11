/**
 * Author: Javi Bonafonte
 */

const POKEMON_TYPES = new Set();
POKEMON_TYPES.add("Normal");        POKEMON_TYPES.add("Fire");
POKEMON_TYPES.add("Water");         POKEMON_TYPES.add("Grass");
POKEMON_TYPES.add("Electric");      POKEMON_TYPES.add("Ice");
POKEMON_TYPES.add("Fighting");      POKEMON_TYPES.add("Poison");
POKEMON_TYPES.add("Ground");        POKEMON_TYPES.add("Flying");
POKEMON_TYPES.add("Psychic");       POKEMON_TYPES.add("Bug");
POKEMON_TYPES.add("Rock");          POKEMON_TYPES.add("Ghost");
POKEMON_TYPES.add("Dragon");        POKEMON_TYPES.add("Dark");
POKEMON_TYPES.add("Steel");         POKEMON_TYPES.add("Fairy");

const POKEMON_TYPES_EFFECT = new Map();
POKEMON_TYPES_EFFECT.set("Normal", [ ["Ghost"], ["Rock", "Steel"], [] ]);
POKEMON_TYPES_EFFECT.set("Fire", [ [], ["Dragon", "Fire", "Rock", "Water"], ["Bug", "Grass", "Ice", "Steel"] ]);
POKEMON_TYPES_EFFECT.set("Water", [ [], ["Dragon", "Grass", "Water"], ["Fire", "Ground", "Rock"] ]);
POKEMON_TYPES_EFFECT.set("Grass", [ [], ["Bug", "Dragon", "Fire", "Flying", "Grass", "Poison", "Steel"], ["Ground", "Rock", "Water"] ]);
POKEMON_TYPES_EFFECT.set("Electric", [ ["Ground"], ["Dragon", "Electric", "Grass"], ["Flying", "Water"] ]);
POKEMON_TYPES_EFFECT.set("Ice", [ [], ["Fire", "Ice", "Steel", "Water"], ["Dragon", "Flying", "Grass", "Ground"] ]);
POKEMON_TYPES_EFFECT.set("Fighting", [ ["Ghost"], ["Bug", "Fairy", "Flying", "Poison", "Psychic"], ["Dark", "Ice", "Normal", "Rock", "Steel"] ]);
POKEMON_TYPES_EFFECT.set("Poison", [ ["Steel"], ["Ghost", "Ground", "Poison", "Rock"], ["Fairy", "Grass"] ]);
POKEMON_TYPES_EFFECT.set("Ground", [ ["Flying"], ["Bug", "Grass"], ["Electric", "Fire", "Poison", "Rock", "Steel"] ]);
POKEMON_TYPES_EFFECT.set("Flying", [ [], ["Electric", "Rock", "Steel"], ["Bug", "Fighting", "Grass"] ]);
POKEMON_TYPES_EFFECT.set("Psychic", [ ["Dark"], ["Psychic", "Steel"], ["Fighting", "Poison"] ]);
POKEMON_TYPES_EFFECT.set("Bug", [ [], ["Fairy", "Fighting", "Fire", "Flying", "Ghost", "Poison", "Steel"], ["Dark", "Grass", "Psychic"] ]);
POKEMON_TYPES_EFFECT.set("Rock", [ [], ["Fighting", "Ground", "Steel"], ["Bug", "Fire", "Flying", "Ice"] ]);
POKEMON_TYPES_EFFECT.set("Ghost", [ ["Normal"], ["Dark"], ["Ghost", "Psychic"] ]);
POKEMON_TYPES_EFFECT.set("Dragon", [ ["Fairy"], ["Steel"], ["Dragon"] ]);
POKEMON_TYPES_EFFECT.set("Dark", [ [], ["Dark", "Fairy", "Fighting"], ["Ghost", "Psychic"] ]);
POKEMON_TYPES_EFFECT.set("Steel", [ [], ["Electric", "Fire", "Steel", "Water"], ["Fairy", "Ice", "Rock"] ]);
POKEMON_TYPES_EFFECT.set("Fairy", [ [], ["Fire", "Poison", "Steel"], ["Dark", "Dragon", "Fighting"] ]);

function GetTypesEffectivenessAgainstTypes(types) {
    let effectiveness = new Map();
    for (let attack_type of POKEMON_TYPES) {
        effectiveness.set(attack_type, GetEffectivenessMultAgainst(attack_type, types));
    }
    return effectiveness;
}

function GetTypesEffectivenessSingleBoost(type) {
    let effectiveness = new Map();
    for (let attacker_type of POKEMON_TYPES) {
        if (attacker_type == type)
            effectiveness.set(attacker_type, Math.fround(1.60));
        else
            effectiveness.set(attacker_type, 1);
    }
    return effectiveness;
}

function GetEffectivenessMultOfType(effectiveness, type) {
    return effectiveness.get(type);
}

function GetEffectivenessMultAgainst(attack_type, enemy_types) {
    const type_effect = POKEMON_TYPES_EFFECT.get(attack_type);
    let mult = 1;
    for (let type of enemy_types) {
        if (type_effect[0].includes(type))
            mult *= 0.390625;
        else if (type_effect[1].includes(type))
            mult *= 0.625;
        else if (type_effect[2].includes(type))
            mult *= Math.fround(1.60);
    }
    return mult;
}

function GetHiddenPowerTypes(hidden_power_filter, pkm_obj) {
    switch (hidden_power_filter) {
        case "None": return [];
        case "Raid Boss": return ["Fighting"];
        case "Type-Match":
            if (pkm_obj && pkm_obj.types)
                return pkm_obj.types.filter(t=>t!="Fairy"&&t!="Normal");
        case "All":
        default:
            return Array.from(POKEMON_TYPES).filter(t=>t!="Fairy"&&t!="Normal");
    }
}

const CPM = [0,0.094,0.16639787,0.21573247,0.25572005,0.29024988,0.3210876,0.34921268,0.3752356,0.39956728,0.4225,0.44310755,0.4627984,0.48168495,0.49985844,0.51739395,0.5343543,0.5507927,0.5667545,0.5822789,0.5974,0.6121573,0.6265671,0.64065295,0.65443563,0.667934,0.6811649,0.69414365,0.7068842,0.7193991,0.7317,0.7377695,0.74378943,0.74976104,0.7556855,0.76156384,0.76739717,0.7731865,0.77893275,0.784637,0.7903,0.7953,0.8003,0.8053,0.8103,0.8153,0.8203,0.8253,0.8303,0.8353,0.8403,0.8453,0.8503,0.8553,0.8603,0.8653];

function GetCPMForLevel(level) {
    if (Number.isInteger(level) && level >= 1 && level < CPM.length)
        return Math.fround(CPM[level]);
    const cpmPrev = GetCPMForLevel(Math.floor(level));
    const cpmNext = GetCPMForLevel(Math.ceil(level));
    return Math.sqrt((cpmPrev**2 + cpmNext**2)/2);
}

function GetPokemonForms(pokemon_id) {
    switch (pokemon_id) {
        case 6: return ["Normal", "Mega", "MegaY"];
        case 359: case 445: case 448: return ["Normal", "Mega", "MegaZ"];
        case 150: return ["Normal", "Mega", "MegaY", "A"];
        case 26: return ["Normal", "Alola", "Mega", "MegaY"];
        case 80: return ["Normal", "Galarian", "Mega"];
        case 3: case 9: case 15: case 18: case 36: case 65: case 71: case 94: case 115: case 121: case 127: case 130: case 142: case 149: case 154: case 160: case 181: case 208: case 212: case 214: case 229: case 248: case 254: case 257: case 260: case 277: case 282: case 302: case 303: case 306: case 308: case 310: case 319: case 323: case 334: case 354: case 358: case 362: case 373: case 376: case 380: case 381: case 382: case 383: case 384: case 398: case 428: case 460: case 475: case 478: case 485: case 491: case 500: case 530: case 531: case 545: case 560: case 604: case 609: case 623: case 652: case 655: case 658: case 687: case 689: case 691: case 701: case 719: case 740: case 768: case 780: case 801: case 807: case 870: case 952: case 970: case 998: return ["Normal", "Mega"];
        case 19: case 20: case 27: case 28: case 37: case 38: case 50: case 51: case 53: case 74: case 75: case 76: case 88: case 89: case 103: case 105: return ["Normal", "Alola"];
        case 77: case 78: case 79: case 83: case 110: case 122: case 144: case 145: case 146: case 199: case 222: case 263: case 264: case 554: case 562: case 618: return ["Normal", "Galarian"];
        case 52: return ["Normal", "Alola", "Galarian"];
        case 58: case 59: case 100: case 101: case 157: case 211: case 215: case 503: case 549: case 570: case 571: case 628: case 705: case 706: case 713: case 724: return ["Normal", "Hisuian"];
        case 194: return ["Normal", "Paldea"];
        case 128: return ["Normal", "Paldea_combat", "Paldea_aqua", "Paldea_blaze"];
        case 201: return ["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Exclamation_point","Question_mark"];
        case 249: case 250: return ["Normal", "S"];
        case 351: return ["Normal", "Sunny", "Rainy", "Snowy"];
        case 386: return ["Normal", "Attack", "Defense", "Speed"];
        case 412: case 413: return ["Plant", "Sandy", "Trash"];
        case 421: return ["Overcast", "Sunny"];
        case 422: case 423: return ["West_sea", "East_sea"];
        case 479: return ["Normal", "Heat", "Wash", "Frost", "Fan", "Mow"];
        case 483: case 484: return ["Normal", "Origin"];
        case 487: return ["Altered", "Origin"];
        case 492: return ["Land", "Sky"];
        case 550: return ["Red_striped", "Blue_striped", "White_striped"];
        case 555: return ["Standard", "Zen", "Galarian_standard", "Galarian_zen"];
        case 585: case 586: return ["Spring", "Summer", "Autumn", "Winter"];
        case 592: case 593: return ["Normal", "Female"];
        case 641: case 642: case 645: case 905: return ["Incarnate", "Therian"];
        case 646: return ["Normal", "White", "Black"];
        case 647: return ["Ordinary", "Resolute"];
        case 648: return ["Aria", "Pirouette"];
        case 649: return ["Normal", "Shock", "Burn", "Chill", "Douse"];
        case 666: return ["Meadow","Archipelago","Continental","Elegant","Fancy","Garden","High_plains","Icy_snow","Jungle","Marine","Modern","Monsoon","Ocean","Poke_ball","Polar","River","Sandstorm","Savanna","Sun","Tundra"];
        case 669: case 671: return ["Red", "Yellow", "Orange", "Blue", "White"];
        case 670: return ["Red", "Yellow", "Orange", "Blue", "White", "Mega"];
        case 676: return ["Natural","Heart","Star","Diamond","Debutante","Matron","Dandy","La_reine","Kabuki","Pharaoh"];
        case 681: return ["Normal", "Blade"];
        case 710: case 711: return ["Average", "Small", "Large", "Super"];
        case 720: return ["Confined", "Unbound"];
        case 718: return ["Fifty_percent", "Ten_percent", "Complete", "Mega"];
        case 741: return ["Baile", "Pompom", "Pau", "Sensu"];
        case 745: return ["Midday", "Midnight", "Dusk"];
        case 746: return ["Solo", "School"];
        case 800: return ["Normal", "Dawn_wings", "Dusk_mane", "Ultra"];
        case 849: return ["Amped", "Low_key"];
        case 854: case 855: return ["Phony", "Antique"];
        case 875: return ["Ice", "Noice"];
        case 876: return ["Male", "Female"];
        case 877: return ["Full_belly", "Hangry"];
        case 888: return ["Hero", "Crowned_sword"];
        case 889: return ["Hero", "Crowned_shield"];
        case 890: return ["Normal", "Eternamax"];
        case 892: return ["Single_strike", "Rapid_strike"];
        case 898: return ["Normal", "Ice_rider", "Shadow_rider"];
        case 902: return ["Normal", "Female"];
        case 916: return ["Normal", "Female"];
        case 925: return ["Family_of_four", "Family_of_three"];
        case 931: return ["Green", "Blue", "Yellow", "White"];
        case 964: return ["Zero", "Hero"];
        case 978: return ["Curly", "Droopy", "Stretchy", "Mega"];
        case 982: return ["Two", "Three"];
        case 1012: return ["Counterfeit", "Artisan"];
        case 1013: return ["Unremarkable", "Masterpiece"];
        default: return ["Normal"];
    }
}

function GetPokemonDefaultForm(pokemon_id) {
    return GetPokemonForms(pokemon_id)[0];
}

function GetPokemonImgSrcName(pokemon_id, form) {
    let poke_name = UntranslatedSpeciesName(pokemon_id);
    if (pokemon_id == 29) poke_name = "nidoranf";
    else if (pokemon_id == 32) poke_name = "nidoranm";
    let img_src_name = poke_name;
    if (form != "Normal") {
        if (form == "Mega" && (pokemon_id == 382 || pokemon_id == 383)) form = "Primal";
        if (form == "Mega" && (pokemon_id == 6 || pokemon_id == 26 || pokemon_id == 150)) form = "MegaX";
        img_src_name += "-" + form.toLowerCase().replace(/_/g, "");
    }
    return img_src_name;
}
