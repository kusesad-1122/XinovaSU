from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "manager/app/src/main/java/com/xinsu/moe"
errors: list[str] = []


def text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def reject(path: Path, pattern: str, message: str) -> None:
    source = text(path)
    match = re.search(pattern, source, re.MULTILINE | re.DOTALL)
    if match:
        line = source.count("\n", 0, match.start()) + 1
        errors.append(f"{path.relative_to(ROOT)}:{line}: {message}")


search_bar = SRC / "ui/component/material/SearchBar.kt"
reject(
    search_bar,
    r"\bSurface\s*\{\s*Column\s*\(",
    "SearchAppBar root Surface must not be opaque",
)

tonal_card = SRC / "ui/component/material/TonalCard.kt"
reject(
    tonal_card,
    r"\bLocalCardOpacity\b",
    "TonalCard must consume the resolved theme surface",
)

home_miuix = SRC / "ui/screen/home/HomeMiuix.kt"
reject(
    home_miuix,
    r"\bcardOpacityFraction\s*\(",
    "HomeMiuix must not apply card opacity locally",
)

raw_card_allowed = {
    SRC / "ui/component/miuix/MiuixGlassCard.kt",
}
for path in sorted((SRC / "ui").rglob("*.kt")):
    if path in raw_card_allowed:
        continue
    reject(
        path,
        r"import\s+top\.yukonga\.miuix\.kmp\.basic\.Card\s*$",
        "use MiuixGlassCard instead of raw Miuix Card",
    )

fixed_top_bars = [
    SRC / "ui/screen/settings/SettingsMaterial.kt",
    SRC / "ui/screen/install/InstallMaterial.kt",
    SRC / "ui/screen/templateeditor/TemplateEditorMaterial.kt",
]
for path in fixed_top_bars:
    reject(
        path,
        r"(?:containerColor|scrolledContainerColor)\s*=\s*MaterialTheme\.colorScheme\.surface",
        "top bar must use transparentChromeColor",
    )

if errors:
    print("\n".join(errors), file=sys.stderr)
    raise SystemExit(1)

print("theme surface guards: PASS")
