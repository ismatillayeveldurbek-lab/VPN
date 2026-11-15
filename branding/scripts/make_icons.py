"""Generate Android launcher/notification icons and store assets from the Tulpor source logos."""
import os, sys
from PIL import Image, ImageDraw, ImageChops

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "final")
OUT = os.path.join(ROOT, "android-res")
STORE = os.path.join(ROOT, "store")
BG_START, BG_END = (43, 31, 140), (116, 27, 235)  # sampled from the original icon
DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

def clean(path, thresh=40):
    im = Image.open(path).convert("RGBA")
    a = im.getchannel("A").point(lambda v: 0 if v < thresh else v)
    im.putalpha(a)
    return im.crop(a.getbbox())

def gradient(size):
    w = h = size
    g = Image.new("RGB", (w, h))
    px = g.load()
    for y in range(h):
        for x in range(w):
            t = (x + y) / (w + h - 2)
            px[x, y] = tuple(round(s + (e - s) * t) for s, e in zip(BG_START, BG_END))
    return g.convert("RGBA")

def place(symbol, canvas, width_frac):
    """Center symbol on a transparent square canvas, scaled to width_frac of canvas width."""
    sw = round(canvas * width_frac)
    sh = round(symbol.height * sw / symbol.width)
    s = symbol.resize((sw, sh), Image.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas))
    out.alpha_composite(s, ((canvas - sw) // 2, (canvas - sh) // 2))
    return out

def mask(img, kind):
    m = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(m)
    n = img.size[0]
    if kind == "round":
        d.ellipse((0, 0, n - 1, n - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, n - 1, n - 1), radius=round(n * 0.22), fill=255)
    out = img.copy()
    out.putalpha(ImageChops.multiply(img.getchannel("A"), m))
    return out

def save(img, *parts):
    p = os.path.join(*parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p, optimize=True)

color = clean(os.path.join(SRC, "tulpor-symbol-color.webp"))
white = clean(os.path.join(SRC, "tulpor-symbol-white.webp"))
white_solid = Image.new("RGBA", white.size, (255, 255, 255, 0))
white_solid.putalpha(white.getchannel("A"))

BIG = 1024
full = gradient(BIG)
full.alpha_composite(place(color, BIG, 0.78))

for name, f in DENS.items():
    # Adaptive foreground/monochrome: 108dp canvas, symbol kept near the 66dp safe zone.
    fg = round(108 * f)
    save(place(color, fg * 4, 0.48).resize((fg, fg), Image.LANCZOS), OUT, f"mipmap-{name}", "ic_launcher_foreground.png")
    save(place(white_solid, fg * 4, 0.46).resize((fg, fg), Image.LANCZOS), OUT, f"mipmap-{name}", "ic_launcher_monochrome.png")
    # Legacy launcher icons (pre-Android 8): 48dp.
    lg = round(48 * f)
    small = full.resize((lg, lg), Image.LANCZOS)
    save(mask(small, "square"), OUT, f"mipmap-{name}", "ic_launcher.png")
    save(mask(small, "round"), OUT, f"mipmap-{name}", "ic_launcher_round.png")
    # Notification (status bar) icon: 24dp, white silhouette with padding.
    nt = round(24 * f)
    save(place(white_solid, nt * 4, 0.92).resize((nt, nt), Image.LANCZOS), OUT, f"drawable-{name}", "ic_stat_tulpor.png")

# Splash screen icon (Android 12+): 288dp canvas, content must fit the 192dp circle.
save(place(color, 1152, 0.50), OUT, "drawable-nodpi", "splash_icon.png")

save(full.resize((512, 512), Image.LANCZOS).convert("RGB"), STORE, "playstore-icon-512.png")
save(mask(full, "square"), STORE, "tulpor-icon-1024.png")
print("done")
