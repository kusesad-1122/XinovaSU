#!/usr/bin/env python3
"""CI entry point for the checked-in XinovaSU artwork catalog."""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PIPELINE_DIR = ROOT / "tools" / "theme_pipeline"
sys.path.insert(0, str(PIPELINE_DIR))

from xinovasu_theme_pipeline import (  # noqa: E402
    load_json,
    validate_candidate_report,
    validate_manifest_structure,
    validate_resource_files,
)


def main() -> int:
    manifest = load_json(PIPELINE_DIR / "theme_sources.json")
    report = load_json(PIPELINE_DIR / "generated" / "theme_candidates.json")
    catalog_sources = (
        ROOT
        / "manager"
        / "app"
        / "src"
        / "main"
        / "java"
        / "com"
        / "xinsu"
        / "moe"
        / "ui"
        / "theme"
        / "tokens"
        / "BuiltInThemeCatalog.kt",
        ROOT
        / "manager"
        / "app"
        / "src"
        / "main"
        / "java"
        / "com"
        / "xinsu"
        / "moe"
        / "ui"
        / "theme"
        / "tokens"
        / "CuratedThemeCatalog.kt",
    )
    errors = validate_manifest_structure(manifest)
    errors.extend(
        validate_resource_files(
            manifest,
            ROOT / "manager" / "app" / "src" / "main" / "res" / "drawable-nodpi",
        )
    )
    errors.extend(validate_candidate_report(manifest, report))
    if all(path.is_file() for path in catalog_sources):
        source = "\n".join(path.read_text(encoding="utf-8") for path in catalog_sources)
        for record in manifest["themes"]:
            for field in ("id", "resource_name", "sha256"):
                value = record[field]
                needle = f'id = "{value}"' if field == "id" else value
                if source.count(needle) != 1:
                    errors.append(
                        f"{record['id']}: catalog must define {field} exactly once ({value})"
                    )
    else:
        missing = [str(path) for path in catalog_sources if not path.is_file()]
        errors.append(f"missing theme catalog source: {', '.join(missing)}")

    integration_requirements = {
        ROOT / "manager/app/src/main/java/com/xinsu/moe/ui/theme/MaterialTheme.kt": (
            "BuiltInThemeCatalog",
            "toMaterialColorScheme",
        ),
        ROOT / "manager/app/src/main/java/com/xinsu/moe/ui/theme/MiuixTheme.kt": (
            "BuiltInThemeCatalog",
            "toMiuixColorScheme",
        ),
        ROOT / "manager/app/src/main/java/com/xinsu/moe/ui/MainActivity.kt": (
            "LocalThemeTokenBundle provides themeTokenBundle",
            "artworkTokens = themeTokenBundle?.artwork",
            "atmosphereTokens = themeTokenBundle?.atmosphere",
        ),
        ROOT / "manager/app/src/main/java/com/xinsu/moe/ui/viewmodel/SettingsViewModel.kt": (
            "toApplicationPlan()",
            "ContentResolver.SCHEME_ANDROID_RESOURCE",
        ),
        ROOT / "manager/app/src/main/java/com/xinsu/moe/ui/screen/colorpalette/ColorPaletteScreenMaterial.kt": (
            "presetValues = legacySelectableKawaiiPalettes",
        ),
        ROOT / "manager/app/src/main/java/com/xinsu/moe/ui/screen/colorpalette/ColorPaletteScreenMiuix.kt": (
            "presetValues = legacySelectableKawaiiPalettes",
        ),
    }
    for path, required_snippets in integration_requirements.items():
        if not path.is_file():
            errors.append(f"missing theme integration source: {path}")
            continue
        source = path.read_text(encoding="utf-8")
        for snippet in required_snippets:
            if snippet not in source:
                errors.append(f"{path.name}: missing integration guard snippet {snippet!r}")
    if errors:
        print("theme catalog guard: FAIL")
        for error in errors:
            print(f"- {error}")
        return 1
    print("theme catalog guard: PASS (21 themes, resources, hashes, dimensions, candidates)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
