"""Generate every KBitChord branding resource: upstream's wave mark on violet.

Writes flavor resource overlays, so nothing under upstream's `src/main` changes:

    app/src/prod/res  app/src/dev/res     phone app
    tv/src/prod/res   tv/src/dev/res      TV app

Each gets the launcher icon and `app_name` in every locale upstream defines it
in. The icon is adaptive: upstream's own white wave foreground (also the
monochrome layer) on a cyan-to-violet diagonal gradient, so it reads as
BitChord's icon in KBitChord's colours. The TV overlays also get the banner:
the waves on the same gradient, drawn from upstream's foreground path.

Usage (repo root; Python 3 with Pillow and NumPy):

    python kbitchord-branding/make_branding.py <preview-dir>
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

preview_dir = sys.argv[1]

TARGETS = {
    "app/src/prod/res": {"name": "KBitChord", "tv": False},
    "app/src/dev/res": {"name": "KBitChord Dev", "tv": False},
    "tv/src/prod/res": {"name": "KBitChord TV", "tv": True},
    "tv/src/dev/res": {"name": "KBitChord TV Dev", "tv": True},
}
# Every values folder whose strings define app_name. The TV app reuses the
# phone app's resources, so the phone's locales are the TV's too.
UPSTREAM_VALUES = "app/src/main/res"
# Upstream's launcher foreground: the white waves. The icon uses it as is; the
# TV banner is drawn from its path.
UPSTREAM_FOREGROUND = "app/src/main/res/drawable/ic_launcher_foreground.xml"
# Upstream ships its TV banner under these qualifiers, all at 1024x576. An
# overlay only replaces a resource with the same qualifiers, so all are covered.
TV_BANNER_DIRS = ("drawable", "drawable-mdpi", "drawable-hdpi", "drawable-xhdpi")
TV_BANNER_SIZE = (1024, 576)
# The waves' width as a share of the banner's, as on upstream's banner.
BANNER_MARK_WIDTH = 0.18
# Earlier generations of this script wrote these; they are removed.
STALE = ("drawable-nodpi/kbitchord_launcher_foreground.png", "values/kbitchord_launcher.xml")

# Top-left to bottom-right.
GRADIENT_START = "22D3EE"
GRADIENT_END = "C026D3"

ANDROID = "{http://schemas.android.com/apk/res/android}"


def rgb(hex6):
    return tuple(int(hex6[i:i + 2], 16) for i in (0, 2, 4))


def path_polygons(d, steps=32):
    """Flatten an SVG path (M L H V C Z, absolute or relative) into polygons."""
    tokens = re.findall(r"[A-Za-z]|-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?", d)
    polys, cur, x, y, start, cmd, i = [], [], 0.0, 0.0, (0.0, 0.0), None, 0

    def num():
        nonlocal i
        i += 1
        return float(tokens[i - 1])

    while i < len(tokens):
        if tokens[i].isalpha():
            cmd = tokens[i]
            i += 1
            if cmd in "Zz":
                if cur:
                    polys.append(cur)
                cur, (x, y) = [], start
                continue
        if cmd is None:
            raise ValueError("path data does not start with a command")
        rel = cmd.islower()
        c = cmd.upper()
        if c == "M":
            nx, ny = num(), num()
            x, y = (x + nx, y + ny) if rel else (nx, ny)
            if cur:
                polys.append(cur)
            cur, start = [(x, y)], (x, y)
            cmd = "l" if rel else "L"  # further pairs are line-tos
        elif c == "L":
            nx, ny = num(), num()
            x, y = (x + nx, y + ny) if rel else (nx, ny)
            cur.append((x, y))
        elif c == "H":
            nx = num()
            x = x + nx if rel else nx
            cur.append((x, y))
        elif c == "V":
            ny = num()
            y = y + ny if rel else ny
            cur.append((x, y))
        elif c == "C":
            p = [num() for _ in range(6)]
            if rel:
                p = [p[k] + (x if k % 2 == 0 else y) for k in range(6)]
            p0, p1, p2, p3 = (x, y), (p[0], p[1]), (p[2], p[3]), (p[4], p[5])
            for s in np.linspace(0, 1, steps)[1:]:
                u = 1 - s
                cur.append(tuple(u**3 * a + 3 * u * u * s * b + 3 * u * s * s * c_ + s**3 * e
                                 for a, b, c_, e in zip(p0, p1, p2, p3)))
            x, y = p3
        else:
            raise ValueError(f"path command {cmd} is not handled; extend path_polygons")
    if cur:
        polys.append(cur)
    return polys


def upstream_waves():
    """Upstream's foreground path, with its group transform, in viewport units."""
    root = ET.parse(UPSTREAM_FOREGROUND).getroot()
    size = float(root.get(ANDROID + "viewportWidth"))
    group = root.find("group")
    scale = float(group.get(ANDROID + "scaleX", "1")) if group is not None else 1.0
    px = float(group.get(ANDROID + "pivotX", "0")) if group is not None else 0.0
    py = float(group.get(ANDROID + "pivotY", "0")) if group is not None else 0.0
    polys = []
    for path in root.iter("path"):
        for poly in path_polygons(path.get(ANDROID + "pathData")):
            polys.append([(px + (a - px) * scale, py + (b - py) * scale) for a, b in poly])
    return size, polys


def gradient(size):
    """The diagonal gradient, as Android's linear gradient from corner to corner draws it."""
    w, h = size
    y, x = np.mgrid[0:h, 0:w].astype(np.float64)
    t = (x * w + y * h) / (w * w + h * h)
    a, b = np.array(rgb(GRADIENT_START), float), np.array(rgb(GRADIENT_END), float)
    return Image.fromarray((a + (b - a) * t[..., None] + 0.5).astype(np.uint8), "RGB")


def bounds(polys):
    xs = [a for poly in polys for a, _ in poly]
    ys = [b for poly in polys for _, b in poly]
    return min(xs), min(ys), max(xs), max(ys)


def banner_image(polys):
    w, h = TV_BANNER_SIZE
    x0, y0, x1, y1 = bounds(polys)
    k = w * BANNER_MARK_WIDTH / (x1 - x0)
    left, top = (w - (x1 - x0) * k) / 2, (h - (y1 - y0) * k) / 2
    ss = 4  # supersampled, then scaled down for smooth edges
    big = Image.new("L", (w * ss, h * ss), 0)
    draw = ImageDraw.Draw(big)
    for poly in polys:
        draw.polygon([((a - x0) * k * ss + left * ss, (b - y0) * k * ss + top * ss) for a, b in poly], fill=255)
    mask = big.resize(TV_BANNER_SIZE, Image.LANCZOS)
    img = gradient(TV_BANNER_SIZE)
    # A soft shadow under the waves, as on upstream's banner.
    shadow = Image.new("L", TV_BANNER_SIZE, 0)
    shadow.paste(mask, (0, 4))
    shadow = shadow.filter(ImageFilter.GaussianBlur(8)).point(lambda v: int(v * 0.22))
    img.paste((0, 0, 0), (0, 0), shadow)
    img.paste((255, 255, 255), (0, 0), mask)
    return img


def write(res_dir, rel, data):
    path = os.path.join(res_dir, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if isinstance(data, Image.Image):
        data.save(path, optimize=True)
    else:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(data)
    print(path)


def xml_escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


viewport, waves = upstream_waves()
banner = banner_image(waves)

ADAPTIVE_ICON = """<?xml version="1.0" encoding="utf-8"?>
<!-- KBitChord launcher icon: upstream's waves on violet. Generated by kbitchord-branding/make_branding.py. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/kbitchord_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
BACKGROUND = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- KBitChord launcher background. Generated by kbitchord-branding/make_branding.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <path android:pathData="M0,0h1024v1024h-1024z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="1024"
                android:endY="1024"
                android:type="linear">
                <item android:color="#FF{GRADIENT_START}" android:offset="0.0" />
                <item android:color="#FF{GRADIENT_END}" android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
"""

# ---- app_name locales ----
locales = []
for path in sorted(glob.glob(os.path.join(UPSTREAM_VALUES, "values*", "*.xml"))):
    with open(path, encoding="utf-8") as f:
        if re.search(r'<string\s+name="app_name"', f.read()):
            folder = os.path.basename(os.path.dirname(path))
            if folder not in locales:
                locales.append(folder)
print("app_name locales:", " ".join(locales))

for res_dir, target in TARGETS.items():
    for rel in STALE:
        stale = os.path.join(res_dir, rel)
        if os.path.exists(stale):
            os.remove(stale)
            print("removed", stale)
    write(res_dir, "mipmap-anydpi/ic_launcher.xml", ADAPTIVE_ICON)
    write(res_dir, "mipmap-anydpi/ic_launcher_round.xml", ADAPTIVE_ICON)
    write(res_dir, "drawable/kbitchord_launcher_background.xml", BACKGROUND)
    for folder in locales:
        # translatable="false" belongs in the default folder only; lint flags it in a locale.
        attr = ' translatable="false"' if folder == "values" else ""
        write(res_dir, f"{folder}/strings_kbitchord.xml", (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            "<!-- Generated by kbitchord-branding/make_branding.py. -->\n"
            "<resources>\n"
            f'    <string name="app_name"{attr}>{xml_escape(target["name"])}</string>\n'
            "</resources>\n"
        ))
    if target["tv"]:
        for folder in TV_BANNER_DIRS:
            write(res_dir, f"{folder}/tv_banner.png", banner)

# ---- Previews: the icon through round and squircle masks, the banner, the monochrome layer ----
os.makedirs(preview_dir, exist_ok=True)
px = 432  # 108dp at xxxhdpi
s = px / viewport
big = Image.new("L", (px * 4, px * 4), 0)
draw = ImageDraw.Draw(big)
for poly in waves:
    draw.polygon([(a * s * 4, b * s * 4) for a, b in poly], fill=255)
icon_mask = big.resize((px, px), Image.LANCZOS)
icon = gradient((px, px))
icon.paste((255, 255, 255), (0, 0), icon_mask)
inner = icon.crop((px // 6, px // 6, px * 5 // 6, px * 5 // 6))  # the launcher shows the inner 72dp

sheet = Image.new("RGB", (3 * 220 + 40 + 512 + 20, 300), (230, 230, 235))
for i, radius in enumerate((0.5, 0.3, 0.0)):
    size = 200
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * radius), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    sheet.paste(inner.resize((size, size), Image.LANCZOS), (20 + i * 220, 50), mask)
sheet.paste(banner.resize((512, 288), Image.LANCZOS), (3 * 220 + 40, 6))
sheet.save(os.path.join(preview_dir, "kbitchord_branding_preview.png"))
mono = Image.new("RGB", (px, px), (60, 60, 60))
mono.paste((235, 235, 235), (0, 0), icon_mask)
mono.resize((200, 200), Image.LANCZOS).save(os.path.join(preview_dir, "kbitchord_monochrome_preview.png"))
print("previews in", preview_dir)
