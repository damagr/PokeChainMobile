#!/usr/bin/env python3
"""
Genera app/src/main/assets/max_battles_snapshot.json con los rankings de
Combates Max (Dynamax/Gigantamax) de Dittobase para los 18 tipos.

Fuente: https://www.dittobase.com/pokemon-go/best-attackers/max-battles/{type}

Motivo: la app usa esta fuente por HTTP; si la red del dispositivo no puede
llegar a dittobase.com (DNS, VPN, operador), la pantalla se queda cargando.
El snapshot empaquetado en la app permite mostrar los datos aunque la red falle.

Uso:
    python3 tools/generate_max_snapshot.py

Requiere python3 >= 3.8 (sin dependencias externas).
"""

import datetime
import html
import json
import os
import re
import sys
import urllib.request

BASE_URL = "https://www.dittobase.com/pokemon-go/best-attackers/max-battles"
USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
OUT_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "max_battles_snapshot.json",
)

TYPES = [
    "normal", "fire", "water", "electric", "grass", "ice",
    "fighting", "poison", "ground", "flying", "psychic", "bug",
    "rock", "ghost", "dragon", "dark", "steel", "fairy",
]

MAX_MOVE_BY_TYPE = {
    "normal": "Max Strike", "fire": "Max Flare", "water": "Max Geyser",
    "electric": "Max Lightning", "grass": "Max Overgrowth", "ice": "Max Hailstorm",
    "fighting": "Max Knuckle", "poison": "Max Ooze", "ground": "Max Quake",
    "flying": "Max Airstream", "psychic": "Max Mindstorm", "bug": "Max Flutterby",
    "rock": "Max Rockfall", "ghost": "Max Phantasm", "dragon": "Max Wyrmwind",
    "dark": "Max Darkness", "steel": "Max Steelspike", "fairy": "Max Starfall",
}

ROW_RE = re.compile(r'<li[^>]*role="row"[^>]*>(.*?)</li>', re.S)
CELL_RE = re.compile(r'<div[^>]*role="cell"[^>]*>')
IMG_RE = re.compile(r'<img[^>]*>', re.S)
ALT_RE = re.compile(r'alt="([^"]*)"')
SRC_RE = re.compile(r'src="([^"]*)"')
TAG_RE = re.compile(r'<[^>]+>')
SPRITE_RE = re.compile(r'/pokemon/(\d+)')


def text_of(fragment):
    """Texto plano de un fragmento HTML (sin tags, entidades resueltas)."""
    return html.unescape(TAG_RE.sub(" ", fragment)).replace(" ", " ").strip()


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", errors="replace")


def parse_rows(page_html):
    """Extrae las filas de datos de la tabla (ancladas en la celda del sprite)."""
    entries = []
    for row_match in ROW_RE.finditer(page_html):
        row_html = row_match.group(1)
        cells = CELL_RE.split(row_html)[1:]
        if len(cells) < 7:
            continue

        anchor = -1
        sprite_img = None
        for index, cell in enumerate(cells):
            for img in IMG_RE.findall(cell):
                alt = ALT_RE.search(img)
                src = SRC_RE.search(img)
                if not alt or not src:
                    continue
                if alt.group(1).strip() and "/pokemon/" in src.group(1):
                    anchor = index
                    sprite_img = (alt.group(1).strip(), src.group(1))
                    break
            if anchor >= 0:
                break
        if anchor < 1 or anchor + 3 >= len(cells):
            continue

        rank_text = text_of(cells[anchor - 1])
        if not rank_text.isdigit():
            continue

        alt, sprite_url = sprite_img
        dex_match = SPRITE_RE.search(sprite_url)
        dex = int(dex_match.group(1)) if dex_match else 0

        if alt.lower().startswith("gigantamax"):
            form, name = "Gigantamax", alt[len("Gigantamax"):].strip()
        elif alt.lower().startswith("dynamax"):
            form, name = "Dynamax", alt[len("Dynamax"):].strip()
        else:
            form, name = "Normal", alt
        if not name or dex <= 0:
            continue

        move_cell = cells[anchor + 1]
        move_text = text_of(move_cell)
        move_type = ""
        for img in IMG_RE.findall(move_cell):
            alt_match = ALT_RE.search(img)
            src_match = SRC_RE.search(img)
            if alt_match and src_match and "/pokemon/" not in src_match.group(1):
                move_type = alt_match.group(1).strip().lower()
                break
        if form == "Gigantamax":
            max_move = move_text
        else:
            max_move = MAX_MOVE_BY_TYPE.get(move_type, move_text)

        damage_text = text_of(cells[anchor + 3]).replace(",", "")
        try:
            damage = float(damage_text)
        except ValueError:
            continue

        entries.append({
            "rank": int(rank_text),
            "dex": dex,
            "name": name,
            "form": form,
            "cm": max_move,
            "rat": damage,
            "spriteUrl": sprite_url,
        })
    return entries


def main():
    types = {}
    for type_slug in TYPES:
        url = f"{BASE_URL}/{type_slug}"
        try:
            page = fetch(url)
        except Exception as error:  # noqa: BLE001
            print(f"ERROR {type_slug}: {error}", file=sys.stderr)
            return 1
        entries = parse_rows(page)
        if not entries:
            print(f"ERROR {type_slug}: 0 filas parseadas", file=sys.stderr)
            return 1
        entries.sort(key=lambda item: item["rank"])
        types[type_slug] = entries
        print(f"OK {type_slug}: {len(entries)} filas")

    snapshot = {
        "generatedAt": datetime.datetime.now(datetime.timezone.utc)
            .replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source": BASE_URL,
        "types": types,
    }
    with open(OUT_PATH, "w", encoding="utf-8") as handle:
        json.dump(snapshot, handle, ensure_ascii=False, separators=(",", ":"))
    size_kb = os.path.getsize(OUT_PATH) / 1024
    print(f"Escrito {OUT_PATH} ({size_kb:.1f} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
