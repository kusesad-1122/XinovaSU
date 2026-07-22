# XinovaSU offline theme pipeline

This authoring tool inventories the 21 user-provided keyart files and produces deterministic color
candidates. The Android app never analyzes artwork at runtime: checked-in semantic tokens remain the
source of truth, and human-authored role notes explain how candidates map to UI roles.

## Analyze sources

The command needs Pillow on the authoring machine. It filters near-black, near-white, low-chroma,
and likely skin pixels; downsamples to 128 px; applies deterministic median-cut; and de-duplicates
candidates in CIE L*a*b*. It intentionally does not infer body parts or claim that a color came from
hair, eyes, or clothing.

```powershell
python tools/theme_pipeline/xinovasu_theme_pipeline.py analyze `
  --manifest tools/theme_pipeline/theme_sources.json `
  --source-dir E:\二次元 `
  --output tools/theme_pipeline/generated/theme_candidates.json
```

## Validate checked-in resources

This path uses only the Python standard library and is safe for CI:

```powershell
python scripts/check_theme_catalog.py
```

It fails on missing or duplicate records, invalid role notes, resource/hash/byte/dimension drift,
and incomplete pressure-theme candidate output. `rights_status` remains publication-pending until
the repository owner explicitly confirms that these images may ship in the public GPL release.
