from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("xinovasu_theme_pipeline.py")
SPEC = importlib.util.spec_from_file_location("xinovasu_theme_pipeline", MODULE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Unable to load {MODULE_PATH}")
pipeline = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = pipeline
SPEC.loader.exec_module(pipeline)


class ColorMathTest(unittest.TestCase):
    def test_delta_e_is_zero_for_same_color(self) -> None:
        lab = pipeline.rgb_to_lab((74, 108, 156))
        self.assertAlmostEqual(pipeline.delta_e(lab, lab), 0.0, places=7)

    def test_distinct_candidates_reject_near_duplicates(self) -> None:
        candidates = [
            pipeline.ColorCandidate("#5076A8", 0.36, 24.0),
            pipeline.ColorCandidate("#5278AA", 0.32, 23.5),
            pipeline.ColorCandidate("#D88FAE", 0.18, 31.0),
            pipeline.ColorCandidate("#26324B", 0.14, 18.0),
        ]
        chosen = pipeline.choose_distinct_candidates(candidates, limit=3, min_delta_e=12.0)
        self.assertEqual([item.hex for item in chosen], ["#5076A8", "#D88FAE", "#26324B"])

    def test_achromatic_artwork_falls_back_to_midtones(self) -> None:
        pixels = [(0, 0, 0), (48, 48, 48), (128, 128, 128), (250, 250, 250)]
        self.assertEqual(
            pipeline.filter_rankable_pixels(pixels),
            [(48, 48, 48), (128, 128, 128)],
        )


class ManifestValidationTest(unittest.TestCase):
    def test_duplicate_theme_id_is_rejected(self) -> None:
        record = {
            "id": "moonlit-silver-blue",
            "display_name_zh": "月下银蓝",
            "source_filename": "source.jpg",
            "resource_name": "theme_moonlit_silver_blue.jpg",
            "width": 10,
            "height": 20,
            "bytes": 3,
            "sha256": "a" * 64,
            "exif_orientation": 1,
            "rights_status": "user-provided-pending-publication-confirmation",
            "role_notes": {
                "primary": "moonlight blue",
                "secondary": "deep indigo clothing",
                "focus": "violet eyes",
                "surface": "night sky",
            },
        }
        manifest = {"schema": 1, "pipeline_version": "1.0.0", "themes": [record, dict(record)]}
        errors = pipeline.validate_manifest_structure(manifest, expected_count=2)
        self.assertTrue(any("duplicate id" in error for error in errors), errors)

    def test_resource_hash_mismatch_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            resource_dir = root / "res"
            resource_dir.mkdir()
            (resource_dir / "theme_test.jpg").write_bytes(b"actual")
            manifest = {
                "schema": 1,
                "pipeline_version": "1.0.0",
                "themes": [
                    {
                        "id": "test-theme",
                        "display_name_zh": "测试",
                        "source_filename": "source.jpg",
                        "resource_name": "theme_test.jpg",
                        "width": 10,
                        "height": 20,
                        "bytes": 8,
                        "sha256": "0" * 64,
                        "exif_orientation": 1,
                        "rights_status": "user-provided-pending-publication-confirmation",
                        "role_notes": {
                            "primary": "a",
                            "secondary": "b",
                            "focus": "c",
                            "surface": "d",
                        },
                    }
                ],
            }
            errors = pipeline.validate_resource_files(manifest, resource_dir)
            self.assertTrue(any("sha256 mismatch" in error for error in errors), errors)


if __name__ == "__main__":
    unittest.main()
