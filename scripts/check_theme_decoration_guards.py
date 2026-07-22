#!/usr/bin/env python3
"""Guard themed card, energy switch, preview, and signing source contracts."""

from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANAGER_SOURCE = ROOT / "manager/app/src/main/java/com/xinsu/moe"
THEME_TEMPLATE = MANAGER_SOURCE / "ui/theme/ThemeTemplate.kt"
DECORATION_SPEC = MANAGER_SOURCE / "ui/theme/decoration/ThemeDecorationSpec.kt"
DECORATION_CATALOG = MANAGER_SOURCE / "ui/theme/decoration/ThemeDecorationCatalog.kt"
TONAL_CARD = MANAGER_SOURCE / "ui/component/material/TonalCard.kt"
MIUIX_GLASS_CARD = MANAGER_SOURCE / "ui/component/miuix/MiuixGlassCard.kt"
ARTWORK_ORNAMENTS = MANAGER_SOURCE / "ui/component/decoration/ArtworkOrnaments.kt"
PALETTE_ORNAMENTS = MANAGER_SOURCE / "ui/component/decoration/PaletteOrnaments.kt"
ENERGY_ORNAMENTS = MANAGER_SOURCE / "ui/component/decoration/EnergyOrnaments.kt"
ENERGY_SWITCH_VISUAL = MANAGER_SOURCE / "ui/component/decoration/EnergySwitchVisual.kt"
ENERGY_PULSE_HOST = MANAGER_SOURCE / "ui/component/decoration/EnergyPulseHost.kt"
CARD_GEOMETRY = MANAGER_SOURCE / "ui/theme/decoration/CardDecorationGeometry.kt"
ENERGY_TIMELINE = MANAGER_SOURCE / "ui/theme/decoration/EnergyTimeline.kt"
CARD_GEOMETRY_TEST = (
    ROOT / "manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/CardDecorationGeometryTest.kt"
)
ENERGY_TIMELINE_TEST = (
    ROOT / "manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/EnergyTimelineTest.kt"
)
ENERGY_TRANSITION_TEST = (
    ROOT / "manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/EnergyTransitionReducerTest.kt"
)
ENERGY_PULSE_HOST_TEST = (
    ROOT / "manager/app/src/test/java/com/xinsu/moe/ui/component/decoration/EnergyPulseHostStateTest.kt"
)
PREVIEW_MATRIX = (
    ROOT
    / "manager/app/src/debug/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationPreviewMatrix.kt"
)
KBUILD = ROOT / "kernel/Kbuild"

MATERIAL_SWITCH_OWNER = MANAGER_SOURCE / "ui/component/material/ExpressiveSwitch.kt"
MIUIX_SWITCH_OWNER = MANAGER_SOURCE / "ui/component/miuix/EnergyMiuixSwitch.kt"

EXPECTED_SIZE_ANCHOR = "XNSU_EXPECTED_SIZE := 0x037d"
EXPECTED_HASH_ANCHOR = (
    "XNSU_EXPECTED_HASH := "
    "4861a86778da1deb60f19391f97ea2c01c1458de20662ff78e303127cdf1731a"
)
MAKE_PREFIX_WORDS = ("override", "export", "unexport", "private")
MAKE_PREFIX_PATTERN = r"(?:(?:override|export|unexport|private)[ \t]+)*"
MAKE_ASSIGNMENT_PATTERN = r":::=|::=|:=|!=|\?=|\+=|="

errors: list[str] = []


def source(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        errors.append(f"missing required source: {path.relative_to(ROOT)}")
        return ""


def report(path: Path, message: str, position: int = 0) -> None:
    text = source(path)
    line = text.count("\n", 0, max(position, 0)) + 1 if text else 1
    errors.append(f"{path.relative_to(ROOT)}:{line}: {message}")


def _quoted_end(text: str, start: int) -> int:
    """Return the exclusive end of a Kotlin string or character literal."""
    if text.startswith('"""', start):
        closing = text.find('"""', start + 3)
        return len(text) if closing < 0 else closing + 3

    quote = text[start]
    index = start + 1
    while index < len(text):
        if text[index] == "\\":
            index += 2
            continue
        if text[index] == quote:
            return index + 1
        index += 1
    return len(text)


def strip_kotlin_comments(text: str) -> str:
    """Blank Kotlin comments while preserving source length and string literals."""
    result = list(text)
    index = 0
    while index < len(text):
        if text.startswith("//", index):
            end = index + 2
            while end < len(text) and text[end] not in "\r\n":
                end += 1
            for position in range(index, end):
                result[position] = " "
            index = end
            continue

        if text.startswith("/*", index):
            depth = 1
            end = index + 2
            while end < len(text) and depth:
                if text.startswith("/*", end):
                    depth += 1
                    end += 2
                elif text.startswith("*/", end):
                    depth -= 1
                    end += 2
                else:
                    end += 1
            for position in range(index, end):
                if text[position] not in "\r\n":
                    result[position] = " "
            index = end
            continue

        if text.startswith('"""', index) or text[index] in {'"', "'"}:
            index = _quoted_end(text, index)
            continue
        index += 1
    return "".join(result)


def mask_kotlin_strings(text: str) -> str:
    """Blank Kotlin string/character literals while preserving source positions."""
    result = list(text)
    index = 0
    while index < len(text):
        if text.startswith('"""', index) or text[index] in {'"', "'"}:
            end = _quoted_end(text, index)
            for position in range(index, end):
                if text[position] not in "\r\n":
                    result[position] = " "
            index = end
            continue
        index += 1
    return "".join(result)


def balanced_body(
    clean_text: str,
    structural_text: str,
    opening: int,
    opener: str,
    closer: str,
) -> tuple[str, int, int] | None:
    if opening < 0 or opening >= len(structural_text) or structural_text[opening] != opener:
        return None
    depth = 0
    for index in range(opening, len(structural_text)):
        character = structural_text[index]
        if character == opener:
            depth += 1
        elif character == closer:
            depth -= 1
            if depth == 0:
                return clean_text[opening + 1 : index], opening + 1, index
    return None


def object_body(path: Path, object_name: str) -> tuple[str, int] | None:
    raw = source(path)
    if not raw:
        return None
    clean = strip_kotlin_comments(raw)
    structural = mask_kotlin_strings(clean)
    matches = list(re.finditer(rf"\bobject\s+{re.escape(object_name)}\b\s*\{{", structural))
    if len(matches) != 1:
        report(path, f"expected one real object {object_name}; found {len(matches)}")
        return None
    opening = structural.find("{", matches[0].start(), matches[0].end())
    region = balanced_body(clean, structural, opening, "{", "}")
    if region is None:
        report(path, f"could not parse object body for {object_name}", matches[0].start())
        return None
    body, start, _ = region
    return body, start


def call_body(
    path: Path,
    container: str,
    pattern: str,
    label: str,
) -> str | None:
    structural = mask_kotlin_strings(container)
    matches = list(re.finditer(pattern, structural, re.MULTILINE))
    if len(matches) != 1:
        report(path, f"expected one real {label} declaration; found {len(matches)}")
        return None
    opening = structural.find("(", matches[0].start(), matches[0].end())
    region = balanced_body(container, structural, opening, "(", ")")
    if region is None:
        report(path, f"could not parse {label} declaration body", matches[0].start())
        return None
    return region[0]


def function_bodies(path: Path, function_name: str) -> list[tuple[str, int]]:
    raw = source(path)
    if not raw:
        return []
    clean = strip_kotlin_comments(raw)
    structural = mask_kotlin_strings(clean)
    pattern = re.compile(
        rf"\bfun\s+(?:[A-Za-z_][A-Za-z0-9_.<>?]*\s*\.\s*)?"
        rf"{re.escape(function_name)}\s*\("
    )
    bodies: list[tuple[str, int]] = []
    for match in pattern.finditer(structural):
        parameter_opening = match.end() - 1
        parameters = balanced_body(clean, structural, parameter_opening, "(", ")")
        if parameters is None:
            report(path, f"could not parse parameters for {function_name}", match.start())
            continue
        parameter_closing = parameters[2]
        body_opening = structural.find("{", parameter_closing + 1)
        if body_opening < 0 or "=" in structural[parameter_closing + 1 : body_opening]:
            report(path, f"{function_name} must have a block body", match.start())
            continue
        region = balanced_body(clean, structural, body_opening, "{", "}")
        if region is None:
            report(path, f"could not parse body for {function_name}", match.start())
            continue
        body, start, _ = region
        bodies.append((body, start))
    return bodies


def named_when_body(
    path: Path,
    function_name: str,
    subject_name: str,
) -> tuple[str, int] | None:
    bodies = function_bodies(path, function_name)
    if len(bodies) != 1:
        report(path, f"expected one real {function_name} function; found {len(bodies)}")
        return None
    function_body, function_start = bodies[0]
    structural = mask_kotlin_strings(function_body)
    matches = list(
        re.finditer(
            rf"\bwhen\s*\(\s*{re.escape(subject_name)}\s*\)\s*\{{",
            structural,
        )
    )
    if len(matches) != 1:
        report(path, f"expected one when ({subject_name}) in {function_name}; found {len(matches)}")
        return None
    opening = structural.find("{", matches[0].start(), matches[0].end())
    region = balanced_body(function_body, structural, opening, "{", "}")
    if region is None:
        report(path, f"could not parse when ({subject_name}) in {function_name}")
        return None
    body, start, _ = region
    return body, function_start + start


def named_call_arguments(body: str, call_name: str) -> list[tuple[str, int]]:
    structural = mask_kotlin_strings(body)
    calls: list[tuple[str, int]] = []
    for match in re.finditer(rf"\b{re.escape(call_name)}\s*\(", structural):
        opening = match.end() - 1
        region = balanced_body(body, structural, opening, "(", ")")
        if region is not None:
            calls.append((region[0], match.start()))
    return calls


def literal_first_arguments(body: str, call_name: str) -> list[str]:
    values: list[str] = []
    for arguments, _ in named_call_arguments(body, call_name):
        match = re.match(r'\s*"([^"\\]*(?:\\.[^"\\]*)*)"', arguments)
        if match is not None:
            values.append(match.group(1))
    return values


def assert_unique_ids() -> None:
    built_in_object = object_body(THEME_TEMPLATE, "BuiltInThemes")
    decoration_object = object_body(DECORATION_CATALOG, "ThemeDecorationCatalog")
    if built_in_object is None or decoration_object is None:
        return

    built_in_declaration = call_body(
        THEME_TEMPLATE,
        built_in_object[0],
        r"\bval\s+all\s*:\s*List\s*<\s*ThemeTemplate\s*>\s*=\s*listOf\s*\(",
        "BuiltInThemes.all",
    )
    decoration_declaration = call_body(
        DECORATION_CATALOG,
        decoration_object[0],
        r"\bprivate\s+val\s+AUTHORED_RECIPES\s*=\s*listOf\s*\(",
        "ThemeDecorationCatalog.AUTHORED_RECIPES",
    )
    if built_in_declaration is None or decoration_declaration is None:
        return

    built_in_ids = literal_first_arguments(built_in_declaration, "token")
    built_in_ids.extend(
        literal_first_arguments(built_in_declaration, "ThemeTemplate")
    )
    decoration_ids = literal_first_arguments(decoration_declaration, "AuthoredRecipe")

    for label, ids, path in (
        ("BuiltInThemes.all", built_in_ids, THEME_TEMPLATE),
        ("ThemeDecorationCatalog", decoration_ids, DECORATION_CATALOG),
    ):
        if len(ids) != 32:
            report(path, f"{label} must register exactly 32 ids; found {len(ids)}")
        duplicates = sorted({theme_id for theme_id in ids if ids.count(theme_id) > 1})
        if duplicates:
            report(path, f"{label} contains duplicate ids: {', '.join(duplicates)}")

    built_in_set = set(built_in_ids)
    decoration_set = set(decoration_ids)
    if built_in_set != decoration_set:
        missing = sorted(built_in_set - decoration_set)
        extra = sorted(decoration_set - built_in_set)
        report(
            DECORATION_CATALOG,
            "theme id sets differ"
            f"; missing decoration ids={missing or 'none'}"
            f"; unexpected decoration ids={extra or 'none'}",
        )


def reject_raw_switch_preference() -> None:
    import_pattern = re.compile(
        r"^\s*import\s+top\.yukonga\.miuix\.kmp\.[\w.]*SwitchPreference"
        r"(?:\s+as\s+[A-Za-z][A-Za-z0-9_]*)?\s*$",
        re.MULTILINE,
    )
    call_pattern = re.compile(r"(?<![\w])SwitchPreference\s*\(")
    for path in sorted((MANAGER_SOURCE / "ui").rglob("*.kt")):
        text = mask_kotlin_strings(strip_kotlin_comments(source(path)))
        for pattern in (import_pattern, call_pattern):
            match = pattern.search(text)
            if match:
                report(
                    path,
                    "business UI must use EnergyMiuixSwitchPreference, not raw SwitchPreference",
                    match.start(),
                )
                break


def reject_raw_switches() -> None:
    material_import = re.compile(
        r"^\s*import\s+androidx\.compose\.material3\.(?:Switch|\*)"
        r"(?:\s+as\s+[A-Za-z][A-Za-z0-9_]*)?\s*$",
        re.MULTILINE,
    )
    material_qualified_call = re.compile(r"androidx\.compose\.material3\.Switch\s*\(")
    miuix_import = re.compile(
        r"^\s*import\s+top\.yukonga\.miuix\.kmp\.basic\.(?:Switch|\*)"
        r"(?:\s+as\s+[A-Za-z][A-Za-z0-9_]*)?\s*$",
        re.MULTILINE,
    )
    miuix_qualified_call = re.compile(r"top\.yukonga\.miuix\.kmp\.basic\.Switch\s*\(")

    for path in sorted((MANAGER_SOURCE / "ui").rglob("*.kt")):
        text = mask_kotlin_strings(strip_kotlin_comments(source(path)))
        material_match = material_import.search(text) or material_qualified_call.search(text)
        if material_match and path != MATERIAL_SWITCH_OWNER:
            report(path, "raw Material3 Switch is only allowed in ExpressiveSwitch.kt", material_match.start())

        miuix_match = miuix_import.search(text) or miuix_qualified_call.search(text)
        if miuix_match and path != MIUIX_SWITCH_OWNER:
            report(path, "raw Miuix Switch is only allowed in EnergyMiuixSwitch.kt", miuix_match.start())

    material_owner = mask_kotlin_strings(
        strip_kotlin_comments(source(MATERIAL_SWITCH_OWNER))
    )
    if material_owner and not (
        material_import.search(material_owner)
        and re.search(r"(?<![\w.])Switch\s*\(", material_owner)
    ):
        report(MATERIAL_SWITCH_OWNER, "ExpressiveSwitch must own the raw Material3 Switch call")

    miuix_owner = mask_kotlin_strings(strip_kotlin_comments(source(MIUIX_SWITCH_OWNER)))
    if miuix_owner and not miuix_qualified_call.search(miuix_owner):
        report(MIUIX_SWITCH_OWNER, "EnergyMiuixSwitch must own the raw Miuix Switch call")


def assert_decorated_card_wrappers() -> None:
    for path in (TONAL_CARD, MIUIX_GLASS_CARD):
        bodies = function_bodies(path, path.stem)
        if not bodies:
            report(path, f"expected at least one real {path.stem} function")
            continue
        for index, (body, start) in enumerate(bodies, start=1):
            structural_body = mask_kotlin_strings(body)
            if not re.search(r"\bDecoratedCardContent\s*\(", structural_body):
                report(
                    path,
                    f"{path.stem} overload {index} must call DecoratedCardContent in its function body",
                    start,
                )


def ornament_enum_values() -> set[str]:
    raw = source(DECORATION_SPEC)
    clean = strip_kotlin_comments(raw)
    structural = mask_kotlin_strings(clean)
    match = re.search(r"\benum\s+class\s+OrnamentMotif\s*\{", structural)
    if match is None:
        report(DECORATION_SPEC, "could not parse OrnamentMotif enum")
        return set()
    opening = structural.find("{", match.start(), match.end())
    region = balanced_body(clean, structural, opening, "{", "}")
    if region is None:
        report(DECORATION_SPEC, "could not parse OrnamentMotif enum body", match.start())
        return set()
    return {
        item.strip()
        for item in region[0].split(",")
        if re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", item.strip())
    }


def explicit_draw_branches(
    path: Path,
    function_name: str,
) -> tuple[list[str], tuple[str, int] | None]:
    when_region = named_when_body(path, function_name, "motif")
    if when_region is None:
        return [], None
    when_body = mask_kotlin_strings(when_region[0])
    return (
        re.findall(
            r"OrnamentMotif\.([A-Za-z][A-Za-z0-9_]*)\s*->\s*draw[A-Za-z0-9_]+\s*\(",
            when_body,
        ),
        when_region,
    )


def assert_explicit_ornaments() -> None:
    artwork, artwork_when = explicit_draw_branches(
        ARTWORK_ORNAMENTS, "drawArtworkOrnament"
    )
    palette, palette_when = explicit_draw_branches(
        PALETTE_ORNAMENTS, "drawPaletteOrnament"
    )
    for path, label, branches, when_region, expected in (
        (ARTWORK_ORNAMENTS, "artwork", artwork, artwork_when, 21),
        (PALETTE_ORNAMENTS, "palette", palette, palette_when, 11),
    ):
        if len(branches) != expected or len(set(branches)) != expected:
            report(
                path,
                f"{label} ornament dispatch must contain {expected} unique explicit draw branches; "
                f"found {len(branches)} branches and {len(set(branches))} unique motifs",
            )
        if when_region is not None:
            else_match = re.search(r"\belse\s*->", mask_kotlin_strings(when_region[0]))
            if else_match:
                report(
                    path,
                    "ornament dispatch must not silently fall back through else ->",
                    when_region[1] + else_match.start(),
                )

    motif_values = ornament_enum_values()
    dispatched = set(artwork) | set(palette)
    if motif_values and dispatched != motif_values:
        report(
            DECORATION_SPEC,
            "explicit ornament branches must cover the OrnamentMotif enum exactly"
            f"; missing={sorted(motif_values - dispatched) or 'none'}"
            f"; unexpected={sorted(dispatched - motif_values) or 'none'}",
        )


def assert_preview_matrix() -> None:
    raw = source(PREVIEW_MATRIX)
    if not raw:
        return
    text = strip_kotlin_comments(raw)
    structural = mask_kotlin_strings(text)

    required_patterns = {
        "32-item ThemeDecorationPreviewProvider": (
            r"class\s+ThemeDecorationPreviewProvider\s*:\s*PreviewParameterProvider<ThemeDecorationSpec>",
            r"ThemeDecorationCatalog\.all\.values\.asSequence\(\)",
        ),
        "Material preview entry": (
            r"fun\s+MaterialThemeDecorationPreview\s*\(",
            r"@PreviewParameter\(ThemeDecorationPreviewProvider::class,\s*limit\s*=\s*32\)",
        ),
        "Miuix preview entry": (
            r"fun\s+MiuixThemeDecorationPreview\s*\(",
        ),
    }
    for label, patterns in required_patterns.items():
        for pattern in patterns:
            if not re.search(pattern, structural, re.MULTILINE):
                report(PREVIEW_MATRIX, f"missing {label} contract: {pattern}")

    preview_parameters = re.findall(
        r"@PreviewParameter\(ThemeDecorationPreviewProvider::class,\s*limit\s*=\s*32\)",
        structural,
    )
    if len(preview_parameters) != 2:
        report(PREVIEW_MATRIX, f"both preview entries must enumerate 32 specs; found {len(preview_parameters)}")

    for matrix in ("MaterialPreviewMatrix", "MiuixPreviewMatrix"):
        if len(re.findall(rf"\b{matrix}\s*\(", structural)) != 2:
            report(PREVIEW_MATRIX, f"preview entry must call {matrix} exactly once")

    for role in ("Hero", "Monitor", "Function", "Standard", "Compact"):
        if f"DecoratedCardRole.{role}" not in structural:
            report(PREVIEW_MATRIX, f"preview matrix must display the {role} card role")

    for content_sample in (
        "CPU 37% · Memory 62% · Battery 84%",
        "Long title and description",
    ):
        if content_sample not in text:
            report(PREVIEW_MATRIX, f"preview matrix must include stress content: {content_sample}")

    for matrix, switch in (
        ("MaterialPreviewMatrix", "ExpressiveSwitch"),
        ("MiuixPreviewMatrix", "EnergyMiuixSwitch"),
    ):
        bodies = function_bodies(PREVIEW_MATRIX, matrix)
        if len(bodies) != 1:
            report(PREVIEW_MATRIX, f"expected one real {matrix} function; found {len(bodies)}")
            continue
        body, body_start = bodies[0]
        body_structure = mask_kotlin_strings(body)
        if len(re.findall(r"\bPreviewCardRoles\.forEach\s*\{", body_structure)) != 1:
            report(
                PREVIEW_MATRIX,
                f"{matrix} must enumerate all preview card roles",
                body_start,
            )

        calls = named_call_arguments(body, switch)
        if len(calls) != 3:
            report(
                PREVIEW_MATRIX,
                f"{matrix} must call {switch} exactly three times for off/mid/on; found {len(calls)}",
                body_start,
            )
            continue

        mid_provider = re.search(
            r"\bCompositionLocalProvider\s*\(\s*"
            r"LocalEnergyPreviewElapsedMillis\s+provides\s+"
            r"EnergyTimeline\.MidPreviewMillis\s*,?\s*\)\s*\{",
            body_structure,
        )
        if mid_provider is None:
            report(
                PREVIEW_MATRIX,
                f"{matrix} must provide one fixed mid-energy preview state",
                body_start,
            )
        else:
            opening = body_structure.find("{", mid_provider.start(), mid_provider.end())
            mid_region = balanced_body(body, body_structure, opening, "{", "}")
            mid_calls = [] if mid_region is None else named_call_arguments(mid_region[0], switch)
            if len(mid_calls) != 1:
                report(
                    PREVIEW_MATRIX,
                    f"{matrix} mid-energy provider must own exactly one {switch}; "
                    f"found {len(mid_calls)}",
                    body_start + mid_provider.start(),
                )
            elif not re.search(
                r"\bchecked\s*=\s*true\b",
                mask_kotlin_strings(mid_calls[0][0]),
            ):
                report(
                    PREVIEW_MATRIX,
                    f"{matrix} mid-energy {switch} must use checked = true",
                    body_start + mid_provider.start(),
                )

        checked_values: list[str] = []
        for arguments, call_start in calls:
            argument_structure = mask_kotlin_strings(arguments)
            checked = re.search(r"\bchecked\s*=\s*(false|true)\b", argument_structure)
            if checked is None:
                report(
                    PREVIEW_MATRIX,
                    f"{matrix} {switch} call must use a literal checked state",
                    body_start + call_start,
                )
            else:
                checked_values.append(checked.group(1))
            if not re.search(
                r"\bonCheckedChange\s*=\s*null\b", argument_structure
            ):
                report(
                    PREVIEW_MATRIX,
                    f"{matrix} {switch} calls must be read-only",
                    body_start + call_start,
                )

        if checked_values.count("false") != 1 or checked_values.count("true") != 2:
            report(
                PREVIEW_MATRIX,
                f"{matrix} {switch} states must be one off and two checked (mid/on); "
                f"found {checked_values}",
                body_start,
            )

    forbidden = re.search(
        r"\b(?:Image|AsyncImage|painterResource|rememberBackgroundImageBitmap|mutableStateOf)\b",
        structural,
    )
    if forbidden:
        report(PREVIEW_MATRIX, "preview must not load images or hold writable state", forbidden.start())


def assert_motion_regressions() -> None:
    geometry = mask_kotlin_strings(strip_kotlin_comments(source(CARD_GEOMETRY)))
    energy = mask_kotlin_strings(strip_kotlin_comments(source(ENERGY_ORNAMENTS)))
    artwork = mask_kotlin_strings(strip_kotlin_comments(source(ARTWORK_ORNAMENTS)))
    visual = mask_kotlin_strings(strip_kotlin_comments(source(ENERGY_SWITCH_VISUAL)))
    pulse_host = mask_kotlin_strings(strip_kotlin_comments(source(ENERGY_PULSE_HOST)))
    timeline = mask_kotlin_strings(strip_kotlin_comments(source(ENERGY_TIMELINE)))
    geometry_test = strip_kotlin_comments(source(CARD_GEOMETRY_TEST))
    timeline_test = strip_kotlin_comments(source(ENERGY_TIMELINE_TEST))
    transition_test = strip_kotlin_comments(source(ENERGY_TRANSITION_TEST))
    pulse_host_test = strip_kotlin_comments(source(ENERGY_PULSE_HOST_TEST))

    horizontal_policy = object_body(CARD_GEOMETRY, "HorizontalTrackPolicy")
    if horizontal_policy is not None:
        policy_body = mask_kotlin_strings(horizontal_policy[0])
        for token in ("fun isHorizontal(", "fun resolve("):
            if token not in policy_body:
                report(CARD_GEOMETRY, f"HorizontalTrackPolicy missing contract: {token}")
    for token in ("data class HorizontalTrackGeometry", "fun endXAt(", "fun lengthAt("):
        if token not in geometry:
            report(CARD_GEOMETRY, f"missing pure horizontal endpoint contract: {token}")
    if "HorizontalTrackPolicy.resolve(" not in artwork:
        report(ARTWORK_ORNAMENTS, "ornament cache must resolve horizontal endpoints once")
    if "reverseHorizontalTrack" in energy or re.search(r"\bscale\s*\(\s*-1f", energy):
        report(ENERGY_ORNAMENTS, "horizontal tracks must not use a second whole-canvas mirror")

    horizontal_paths = (
        "BrushSweep", "TwinConverge", "RailCharge", "PawSteps",
        "ElasticConverge", "SlashCut", "RibbonFlip", "FieldSweep", "LampMarch",
        "ThornGrow", "TideFill", "ChoiceConfirm", "VineGrow", "DataPulse",
        "LayerSweep", "FuseBurn",
    )
    render_contract = re.compile(
        r"\bval\s+endX\s*=\s*endXAt\s*\(\s*progress\s*\)"
    )
    if "fun HorizontalTrackGeometry.withRenderCoordinatesAt(" not in geometry:
        report(CARD_GEOMETRY, "missing allocation-free pure horizontal render-coordinate contract")
    if not render_contract.search(geometry):
        report(CARD_GEOMETRY, "horizontal render coordinates must resolve endXAt(progress) exactly")
    mutation = geometry.replace(
        "val endX = endXAt(progress)",
        "val endX = endXAt(1f - progress)",
        1,
    )
    if render_contract.search(mutation):
        report(
            Path(__file__).resolve(),
            "horizontal endpoint mutation probe must reject endXAt(1f - progress)",
        )

    track_bodies = function_bodies(ENERGY_ORNAMENTS, "drawEnergyTrack")
    if len(track_bodies) != 1:
        report(ENERGY_ORNAMENTS, f"expected one drawEnergyTrack; found {len(track_bodies)}")
    else:
        raw_track_body = track_bodies[0][0]
        track_body = mask_kotlin_strings(raw_track_body)
        if "horizontalWidth" in track_body:
            report(ENERGY_ORNAMENTS, "drawEnergyTrack must not rebuild physical-LTR horizontalWidth")
        if re.search(r"\.\s*(?:endXAt|deltaXAt)\s*\(", track_body):
            report(ENERGY_ORNAMENTS, "drawEnergyTrack branches must not bypass pure render coordinates")
        render_call = re.compile(
            r"g\.horizontalTrack\.withRenderCoordinatesAt\s*\(\s*progress\s*\)\s*\{\s*"
            r"horizontalStartX\s*,\s*horizontalEndX\s*,\s*horizontalDeltaX\s*,\s*"
            r"horizontalDirection\s*->"
        )
        calls = list(render_call.finditer(track_body))
        render_body = ""
        if len(calls) != 1:
            report(ENERGY_ORNAMENTS, f"expected one shared horizontal render-plan call; found {len(calls)}")
        else:
            opening = track_body.find("{", calls[0].start(), calls[0].end())
            region = balanced_body(raw_track_body, track_body, opening, "{", "}")
            if region is None:
                report(ENERGY_ORNAMENTS, "could not parse shared horizontal render-plan body")
            else:
                render_body = mask_kotlin_strings(region[0])
        for path in horizontal_paths:
            branches = list(
                re.finditer(
                    rf"EnergyTrack\.{path}\s*->"
                    rf"(?P<body>(?:(?!EnergyTrack\.).)*)",
                    render_body,
                    re.DOTALL,
                )
            )
            if len(branches) != 1:
                report(ENERGY_ORNAMENTS, f"{path} must be inside the shared horizontal render plan")
                continue
            if not re.search(
                r"\bhorizontal(?:StartX|EndX|DeltaX|Direction)\b",
                branches[0].group("body"),
            ):
                report(ENERGY_ORNAMENTS, f"{path} must consume resolved render coordinates")

    for token in (
        "every horizontal track grows monotonically within bounds in ltr and rtl",
        "ribbon flip and choice confirm grow from their logical ornament anchor",
        "listOf(0f, 0.5f, 1f)",
    ):
        if token not in geometry_test:
            report(CARD_GEOMETRY_TEST, f"missing horizontal regression test: {token}")
    if "withRenderCoordinatesAt(progress)" not in geometry_test:
        report(CARD_GEOMETRY_TEST, "horizontal tests must exercise the shared render-coordinate contract")

    linear_tween = re.compile(
        r"animationSpec\s*=\s*tween\s*\(\s*"
        r"durationMillis\s*=\s*plan\.durationMillis\s*,\s*"
        r"easing\s*=\s*LinearEasing\s*,?\s*\)",
        re.DOTALL,
    )
    if "import androidx.compose.animation.core.LinearEasing" not in source(ENERGY_SWITCH_VISUAL):
        report(ENERGY_SWITCH_VISUAL, "outer energy clock must import LinearEasing")
    if not linear_tween.search(visual):
        report(ENERGY_SWITCH_VISUAL, "outer energy clock tween must explicitly use LinearEasing")
    if "outer animation progress is a linear wall clock fraction" not in timeline_test:
        report(ENERGY_TIMELINE_TEST, "missing pure linear wall-clock contract")

    transient_bodies = function_bodies(ENERGY_TIMELINE, "transientAlpha")
    if len(transient_bodies) != 1:
        report(ENERGY_TIMELINE, f"expected one ActivePulsePolicy.transientAlpha; found {len(transient_bodies)}")
    else:
        transient = mask_kotlin_strings(transient_bodies[0][0])
        if not re.search(r"pulse\s*<=\s*0f\s*\|\|\s*pulse\s*>=\s*1f", transient):
            report(ENERGY_TIMELINE, "pulse envelope must be exactly zero at both endpoints")
        if "0.35f +" in transient or "sin(pulse * PI)" not in transient:
            report(ENERGY_TIMELINE, "pulse envelope must be continuous, bounded, and middle-peaked")
    for token in (
        "active and inactive pulse envelopes are continuous bounded and zero at both ends",
        "0.0001f",
        "1f - epsilon",
    ):
        if token not in timeline_test:
            report(ENERGY_TIMELINE_TEST, f"missing pulse envelope regression test: {token}")

    for token in ("fun handoff(", "fun releaseIfSettled(", "fun release("):
        if token not in pulse_host:
            report(ENERGY_PULSE_HOST, f"missing pulse lifecycle API: {token}")
    if "owner == this.owner" not in pulse_host:
        report(ENERGY_PULSE_HOST, "pulse publish and release must remain owner-scoped")
    if "NonCancellable" in visual:
        report(ENERGY_SWITCH_VISUAL, "pulse drain must remain cancellable")
    if not re.search(r"\bDisposableEffect\s*\(\s*pulseHost\s*\)", visual):
        report(ENERGY_SWITCH_VISUAL, "pulse owner must be cleaned on composable disposal")
    launched_effects = named_call_arguments(visual, "LaunchedEffect")
    if not any(re.search(r"\bpulseHost\b", arguments) for arguments, _ in launched_effects):
        report(ENERGY_SWITCH_VISUAL, "pulseHost must participate in LaunchedEffect replacement keys")
    for token in (
        "pulseHost?.handoff(owner)",
        "animateRetainedPulseToRest(",
        "pulseHost?.releaseIfSettled(owner)",
    ):
        if token not in visual:
            report(ENERGY_SWITCH_VISUAL, f"missing continuous pulse replacement contract: {token}")
    if "pulseHost?.release(owner)" in visual:
        report(ENERGY_SWITCH_VISUAL, "animation finally must not unconditionally clear a mid-flight pulse")
    for token in (
        "publish half handoff decays through quarter to settled release",
        "dispose release clears current owner without replacement",
        "host.handoff(replacementOwner)",
        "host.release(oldOwner)",
        "host.releaseIfSettled(replacementOwner)",
        "listOf(false, true)",
    ):
        if token not in pulse_host_test:
            report(ENERGY_PULSE_HOST_TEST, f"missing lifecycle regression test: {token}")
    if "rapid reversal starts from current progress" not in transition_test:
        report(ENERGY_TRANSITION_TEST, "missing checked rapid-reversal reducer contract")


def make_logical_lines(text: str) -> list[tuple[str, int]]:
    logical_lines: list[tuple[str, int]] = []
    buffer = ""
    buffer_start = 0
    offset = 0

    for physical_line in text.splitlines(keepends=True):
        line = physical_line.rstrip("\r\n")
        if not buffer:
            buffer_start = offset
        else:
            line = line.lstrip(" \t")

        trailing_backslashes = len(line) - len(line.rstrip("\\"))
        if trailing_backslashes % 2 == 1:
            buffer += line[:-1] + " "
        else:
            logical_lines.append((buffer + line, buffer_start))
            buffer = ""
        offset += len(physical_line)

    if buffer:
        logical_lines.append((buffer, buffer_start))
    return logical_lines


def strip_make_comment(line: str) -> str:
    for index, character in enumerate(line):
        if character != "#":
            continue
        backslashes = 0
        cursor = index - 1
        while cursor >= 0 and line[cursor] == "\\":
            backslashes += 1
            cursor -= 1
        if backslashes % 2 == 0:
            return line[:index]
    return line


def protected_make_events(
    text: str,
    variable: str,
) -> list[tuple[str, str, str, int]]:
    assignment = re.compile(
        rf"(?<![A-Za-z0-9_])(?P<prefixes>{MAKE_PREFIX_PATTERN})"
        rf"{re.escape(variable)}\b[ \t]*(?P<operator>{MAKE_ASSIGNMENT_PATTERN})"
    )
    directive = re.compile(
        rf"^[ \t]*{MAKE_PREFIX_PATTERN}(?P<directive>define|undefine)"
        rf"[ \t]+{re.escape(variable)}\b"
    )
    events: list[tuple[str, str, str, int]] = []

    for logical_line, position in make_logical_lines(text):
        code = strip_make_comment(logical_line)
        if not code.strip():
            continue

        directive_match = directive.search(code)
        if directive_match is not None:
            events.append(
                (directive_match.group("directive"), "", logical_line, position)
            )
            continue

        assignment_matches = list(assignment.finditer(code))
        if assignment_matches:
            events.extend(
                ("assignment", match.group("operator"), logical_line, position)
                for match in assignment_matches
            )
            continue

        tokens = code.split()
        if variable in tokens:
            variable_index = tokens.index(variable)
            prefixes = tokens[:variable_index]
            if prefixes and all(prefix in MAKE_PREFIX_WORDS for prefix in prefixes):
                if "export" in prefixes or "unexport" in prefixes:
                    events.append(("export-directive", "", logical_line, position))

    return events


def assert_signing_anchors() -> None:
    text = source(KBUILD)
    for variable, expected in (
        ("XNSU_EXPECTED_SIZE", EXPECTED_SIZE_ANCHOR),
        ("XNSU_EXPECTED_HASH", EXPECTED_HASH_ANCHOR),
    ):
        events = protected_make_events(text, variable)
        allowed = [
            event
            for event in events
            if event[0] == "assignment"
            and event[1] == ":="
            and event[2] == expected
        ]
        if len(events) != 1 or len(allowed) != 1:
            invalid = next((event for event in events if event not in allowed), None)
            if invalid is None and len(events) > 1:
                invalid = events[1]
            position = invalid[3] if invalid is not None else 0
            definitions = [
                f"{kind}{f' {operator}' if operator else ''}: {line.strip()}"
                for kind, operator, line, _ in events
            ]
            report(
                KBUILD,
                f"{variable} must have exactly one unprefixed live assignment {expected!r} "
                f"and no other assignment/define/undefine/export directive; "
                f"found {definitions or 'none'}",
                position,
            )


def main() -> int:
    assert_unique_ids()
    reject_raw_switch_preference()
    reject_raw_switches()
    assert_decorated_card_wrappers()
    assert_explicit_ornaments()
    assert_preview_matrix()
    assert_motion_regressions()
    assert_signing_anchors()

    if errors:
        print("theme decoration guards: FAIL", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("theme decoration guards: PASS (32 themes, cards, switches, ornaments, preview, signing)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
