#!/usr/bin/env python3
"""Deterministic offline inventory and color-candidate pipeline for XinovaSU themes.

The checked-in Android runtime never imports Pillow and never analyzes artwork. Pillow is only
loaded by the explicit ``analyze`` command on an authoring machine. CI validation uses the Python
standard library only.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import struct
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Iterable, Sequence


PIPELINE_VERSION = "1.0.0"
PRESSURE_THEME_IDS = {
    "moonlit-silver-blue",
    "sakura-street-walk",
    "sea-breeze-song",
}
REQUIRED_ROLE_NOTES = ("primary", "secondary", "focus", "surface")
THEME_ID_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
RESOURCE_RE = re.compile(r"^theme_[a-z0-9_]+\.jpg$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


@dataclass(frozen=True)
class ColorCandidate:
    hex: str
    population: float
    chroma: float


def _srgb_channel_to_linear(value: float) -> float:
    value /= 255.0
    return value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4


def rgb_to_lab(rgb: tuple[int, int, int]) -> tuple[float, float, float]:
    """Convert 8-bit sRGB to CIE L*a*b* using D65, without third-party dependencies."""

    red, green, blue = (_srgb_channel_to_linear(channel) for channel in rgb)
    x = (red * 0.4124564 + green * 0.3575761 + blue * 0.1804375) / 0.95047
    y = red * 0.2126729 + green * 0.7151522 + blue * 0.0721750
    z = (red * 0.0193339 + green * 0.1191920 + blue * 0.9503041) / 1.08883

    epsilon = 216 / 24389
    kappa = 24389 / 27

    def pivot(value: float) -> float:
        return value ** (1 / 3) if value > epsilon else (kappa * value + 16) / 116

    fx, fy, fz = pivot(x), pivot(y), pivot(z)
    return 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)


def delta_e(first: tuple[float, float, float], second: tuple[float, float, float]) -> float:
    return math.sqrt(sum((left - right) ** 2 for left, right in zip(first, second)))


def hex_to_rgb(value: str) -> tuple[int, int, int]:
    clean = value.removeprefix("#")
    if len(clean) != 6 or not re.fullmatch(r"[0-9A-Fa-f]{6}", clean):
        raise ValueError(f"Invalid RGB hex color: {value}")
    return tuple(int(clean[index : index + 2], 16) for index in (0, 2, 4))  # type: ignore[return-value]


def choose_distinct_candidates(
    candidates: Sequence[ColorCandidate], *, limit: int, min_delta_e: float
) -> list[ColorCandidate]:
    """Preserve score order while removing colors that are perceptually near-duplicates."""

    selected: list[ColorCandidate] = []
    selected_labs: list[tuple[float, float, float]] = []
    for candidate in candidates:
        lab = rgb_to_lab(hex_to_rgb(candidate.hex))
        if all(delta_e(lab, other) >= min_delta_e for other in selected_labs):
            selected.append(candidate)
            selected_labs.append(lab)
        if len(selected) == limit:
            break
    return selected


def read_jpeg_size(path: Path) -> tuple[int, int]:
    """Read JPEG dimensions using SOF markers so CI does not need Pillow."""

    with path.open("rb") as stream:
        if stream.read(2) != b"\xff\xd8":
            raise ValueError("not a JPEG file")
        while True:
            marker_start = stream.read(1)
            if not marker_start:
                raise ValueError("JPEG has no start-of-frame marker")
            if marker_start != b"\xff":
                continue
            marker = stream.read(1)
            while marker == b"\xff":
                marker = stream.read(1)
            if not marker or marker in (b"\xd8", b"\xd9"):
                continue
            if marker == b"\xda":
                raise ValueError("JPEG scan started before dimensions were found")
            length_bytes = stream.read(2)
            if len(length_bytes) != 2:
                raise ValueError("truncated JPEG segment")
            segment_length = struct.unpack(">H", length_bytes)[0]
            if segment_length < 2:
                raise ValueError("invalid JPEG segment length")
            marker_value = marker[0]
            if marker_value in {
                0xC0,
                0xC1,
                0xC2,
                0xC3,
                0xC5,
                0xC6,
                0xC7,
                0xC9,
                0xCA,
                0xCB,
                0xCD,
                0xCE,
                0xCF,
            }:
                payload = stream.read(segment_length - 2)
                if len(payload) < 5:
                    raise ValueError("truncated JPEG start-of-frame segment")
                height, width = struct.unpack(">HH", payload[1:5])
                return width, height
            stream.seek(segment_length - 2, 1)


def validate_manifest_structure(manifest: dict[str, Any], *, expected_count: int = 21) -> list[str]:
    errors: list[str] = []
    if manifest.get("schema") != 1:
        errors.append("manifest schema must be 1")
    if manifest.get("pipeline_version") != PIPELINE_VERSION:
        errors.append(f"pipeline_version must be {PIPELINE_VERSION}")
    themes = manifest.get("themes")
    if not isinstance(themes, list):
        return errors + ["themes must be a list"]
    if len(themes) != expected_count:
        errors.append(f"expected {expected_count} themes, found {len(themes)}")

    seen: dict[str, set[str]] = {"id": set(), "source_filename": set(), "resource_name": set()}
    for index, record in enumerate(themes):
        label = f"themes[{index}]"
        if not isinstance(record, dict):
            errors.append(f"{label} must be an object")
            continue
        for key in (
            "id",
            "display_name_zh",
            "source_filename",
            "resource_name",
            "width",
            "height",
            "bytes",
            "sha256",
            "exif_orientation",
            "rights_status",
            "role_notes",
        ):
            if key not in record:
                errors.append(f"{label} missing {key}")
        theme_id = record.get("id")
        if not isinstance(theme_id, str) or not THEME_ID_RE.fullmatch(theme_id):
            errors.append(f"{label} has invalid id")
        resource_name = record.get("resource_name")
        if not isinstance(resource_name, str) or not RESOURCE_RE.fullmatch(resource_name):
            errors.append(f"{label} has invalid resource_name")
        source_filename = record.get("source_filename")
        if not isinstance(source_filename, str) or not source_filename.lower().endswith(".jpg"):
            errors.append(f"{label} has invalid source_filename")
        sha256 = record.get("sha256")
        if not isinstance(sha256, str) or not SHA256_RE.fullmatch(sha256):
            errors.append(f"{label} has invalid sha256")
        for numeric_key in ("width", "height", "bytes"):
            value = record.get(numeric_key)
            if not isinstance(value, int) or value <= 0:
                errors.append(f"{label} has invalid {numeric_key}")
        if record.get("exif_orientation") != 1:
            errors.append(f"{label} must be normalized to EXIF orientation 1")
        if record.get("rights_status") != "user-provided-pending-publication-confirmation":
            errors.append(f"{label} has unsupported rights_status")
        role_notes = record.get("role_notes")
        if not isinstance(role_notes, dict):
            errors.append(f"{label} role_notes must be an object")
        else:
            for role in REQUIRED_ROLE_NOTES:
                note = role_notes.get(role)
                if not isinstance(note, str) or not note.strip():
                    errors.append(f"{label} missing role note {role}")
        for unique_key in seen:
            value = record.get(unique_key)
            if not isinstance(value, str):
                continue
            if value in seen[unique_key]:
                errors.append(f"duplicate {unique_key}: {value}")
            seen[unique_key].add(value)

    actual_pressure = {record.get("id") for record in themes if record.get("pressure_test") is True}
    if actual_pressure != PRESSURE_THEME_IDS:
        errors.append(
            "pressure_test ids must be exactly " + ", ".join(sorted(PRESSURE_THEME_IDS))
        )
    return errors


def validate_resource_files(manifest: dict[str, Any], resource_dir: Path) -> list[str]:
    errors: list[str] = []
    for record in manifest.get("themes", []):
        if not isinstance(record, dict) or not isinstance(record.get("resource_name"), str):
            continue
        theme_id = record.get("id", "<unknown>")
        path = resource_dir / record["resource_name"]
        if not path.is_file():
            errors.append(f"{theme_id}: missing resource {path}")
            continue
        data = path.read_bytes()
        digest = hashlib.sha256(data).hexdigest()
        if digest != record.get("sha256"):
            errors.append(f"{theme_id}: sha256 mismatch ({digest})")
        if len(data) != record.get("bytes"):
            errors.append(f"{theme_id}: byte-size mismatch ({len(data)})")
        try:
            dimensions = read_jpeg_size(path)
        except ValueError as error:
            errors.append(f"{theme_id}: {error}")
        else:
            expected = (record.get("width"), record.get("height"))
            if dimensions != expected:
                errors.append(f"{theme_id}: dimension mismatch ({dimensions[0]}x{dimensions[1]})")
    return errors


def validate_candidate_report(manifest: dict[str, Any], report: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if report.get("pipeline_version") != PIPELINE_VERSION:
        errors.append("candidate report pipeline_version mismatch")
    rows = report.get("themes")
    if not isinstance(rows, list):
        return errors + ["candidate report themes must be a list"]
    by_id = {row.get("id"): row for row in rows if isinstance(row, dict)}
    manifest_ids = {record.get("id") for record in manifest.get("themes", [])}
    if set(by_id) != manifest_ids:
        errors.append("candidate report theme ids do not match manifest")
    for theme_id in PRESSURE_THEME_IDS:
        candidates = by_id.get(theme_id, {}).get("candidates", [])
        if not isinstance(candidates, list) or len(candidates) < 3:
            errors.append(f"{theme_id}: expected at least 3 hue-distinct candidates")
    return errors


def load_json(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return data


def _looks_like_skin(red: int, green: int, blue: int) -> bool:
    maximum, minimum = max(red, green, blue), min(red, green, blue)
    return red > 95 and green > 40 and blue > 20 and maximum - minimum > 15 and red > green and red > blue


def filter_rankable_pixels(
    pixels: Iterable[tuple[int, int, int]],
) -> list[tuple[int, int, int]]:
    """Prefer chromatic pixels, but preserve midtone neutrals for achromatic artwork."""

    chromatic: list[tuple[int, int, int]] = []
    neutral_fallback: list[tuple[int, int, int]] = []
    for red, green, blue in pixels:
        lab = rgb_to_lab((red, green, blue))
        luminance, chroma = lab[0], math.hypot(lab[1], lab[2])
        if luminance < 8 or luminance > 96 or _looks_like_skin(red, green, blue):
            continue
        neutral_fallback.append((red, green, blue))
        if chroma >= 5:
            chromatic.append((red, green, blue))
    return chromatic or neutral_fallback


def analyze_image(path: Path, *, color_count: int = 20, distinct_count: int = 8) -> list[ColorCandidate]:
    try:
        from PIL import Image
    except ImportError as error:  # pragma: no cover - exercised only on authoring machines
        raise RuntimeError("Pillow is required only for the offline analyze command") from error

    with Image.open(path) as source:
        image = source.convert("RGB")
        image.thumbnail((128, 128))
        raw = image.tobytes()
        pixels = zip(raw[0::3], raw[1::3], raw[2::3])
        filtered = filter_rankable_pixels(pixels)
        if not filtered:
            raise ValueError(f"{path.name}: no eligible pixels after filtering")
        sample = Image.new("RGB", (len(filtered), 1))
        sample.putdata(filtered)
        quantized = sample.quantize(colors=color_count, method=Image.Quantize.MEDIANCUT)
        palette = quantized.getpalette()
        counts = quantized.getcolors() or []
        total = sum(count for count, _ in counts)
        ranked: list[tuple[float, ColorCandidate]] = []
        for count, palette_index in counts:
            offset = palette_index * 3
            rgb = tuple(palette[offset : offset + 3])
            lab = rgb_to_lab(rgb)  # type: ignore[arg-type]
            chroma = math.hypot(lab[1], lab[2])
            population = count / total
            hex_value = "#" + "".join(f"{channel:02X}" for channel in rgb)
            candidate = ColorCandidate(hex_value, round(population, 6), round(chroma, 3))
            ranked.append((population * (1.0 + min(chroma, 80.0) / 100.0), candidate))
        ranked.sort(key=lambda item: (-item[0], item[1].hex))
        return choose_distinct_candidates(
            [candidate for _, candidate in ranked], limit=distinct_count, min_delta_e=12.0
        )


def build_candidate_report(manifest: dict[str, Any], source_dir: Path) -> dict[str, Any]:
    themes = []
    for record in manifest["themes"]:
        path = source_dir / record["source_filename"]
        if not path.is_file():
            raise FileNotFoundError(path)
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if digest != record["sha256"]:
            raise ValueError(f"{record['id']}: source sha256 mismatch")
        width, height = read_jpeg_size(path)
        if (width, height) != (record["width"], record["height"]):
            raise ValueError(f"{record['id']}: source dimension mismatch")
        themes.append(
            {
                "id": record["id"],
                "source_filename": record["source_filename"],
                "candidates": [asdict(candidate) for candidate in analyze_image(path)],
            }
        )
    return {"pipeline_version": PIPELINE_VERSION, "themes": themes}


def _print_errors(errors: Iterable[str]) -> int:
    rows = list(errors)
    if not rows:
        print("theme catalog validation: PASS")
        return 0
    print("theme catalog validation: FAIL")
    for error in rows:
        print(f"- {error}")
    return 1


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    analyze = subparsers.add_parser("analyze", help="produce deterministic color candidates")
    analyze.add_argument("--manifest", type=Path, required=True)
    analyze.add_argument("--source-dir", type=Path, required=True)
    analyze.add_argument("--output", type=Path, required=True)

    validate = subparsers.add_parser("validate", help="validate checked-in catalog resources")
    validate.add_argument("--manifest", type=Path, required=True)
    validate.add_argument("--resource-dir", type=Path, required=True)
    validate.add_argument("--candidate-report", type=Path)

    args = parser.parse_args(argv)
    manifest = load_json(args.manifest)
    if args.command == "analyze":
        errors = validate_manifest_structure(manifest)
        if errors:
            return _print_errors(errors)
        report = build_candidate_report(manifest, args.source_dir)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {len(report['themes'])} theme candidate records to {args.output}")
        return 0

    errors = validate_manifest_structure(manifest)
    errors.extend(validate_resource_files(manifest, args.resource_dir))
    if args.candidate_report:
        errors.extend(validate_candidate_report(manifest, load_json(args.candidate_report)))
    return _print_errors(errors)


if __name__ == "__main__":
    raise SystemExit(main())
