#!/usr/bin/env python3
"""Aggregate open medical text into a canonical corpus and publish it to the HF Hub.

Version label: media-100m-foundation-v1
Target repo:   DOCTOSHI/media-medical-corpus (public)

The token is read from the HF_TOKEN environment variable only. It is never
written, echoed, or logged. Publishing is skipped (with a clear message) when the
token is missing or invalid, so ingestion can still run offline.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import html
import io
import json
import os
import re
import sys
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = ROOT / "datasets" / "medical_corpus"
VERSION = "media-100m-foundation-v1"
TARGET_REPO = "DOCTOSHI/media-medical-corpus"
DEFAULT_MAX_ROWS = 200_000
MIN_CHARS = 100

# Source registry. Each entry maps a source key to dataset id, config, splits,
# licence and attribution. `config` may be None for single-config datasets.
# WikiProject Medicine has no unambiguous, licence-clear Hub extract, so it is
# intentionally omitted and recorded as such in the dataset card.
SOURCES = {
    "medqa_usmle": {
        "dataset_id": "GBaker/MedQA-USMLE-4-options",
        "config": None,
        "splits": ["train", "test"],
        "license": "verify-on-hub",
        "attribution": "MedQA (Jin et al., 2021), USMLE 4-option questions.",
    },
    "pubmedqa_labeled": {
        "dataset_id": "qiaojin/PubMedQA",
        "config": "pqa_labeled",
        "splits": ["train"],
        "license": "verify-on-hub",
        "attribution": "PubMedQA labeled set (Jin et al., 2019).",
    },
    "pubmedqa_artificial": {
        "dataset_id": "qiaojin/PubMedQA",
        "config": "pqa_artificial",
        "splits": ["train"],
        "license": "verify-on-hub",
        "attribution": "PubMedQA artificial set (Jin et al., 2019).",
    },
}

HTML_RE = re.compile(r"<[^>]+>")
CITATION_RE = re.compile(r"\[\d+(?:\s*[,\-]\s*\d+)*\]")
CONTROL_RE = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
WS_RE = re.compile(r"[ \t]+")
MULTINL_RE = re.compile(r"\n{3,}")

MOJIBAKE = {
    "\u00c3\u00a9": "\u00e9", "\u00c3\u00a8": "\u00e8", "\u00c3 ": "\u00e0",
    "\u00e2\u0080\u0099": "'", "\u00e2\u0080\u009c": '"', "\u00e2\u0080\u009d": '"',
    "\u00e2\u0080\u0093": "-", "\u00e2\u0080\u0094": "-", "\u00c2": "",
}


def normalize(text: str) -> str:
    """Apply the spec's normalization steps to a raw medical text snippet."""
    text = html.unescape(text or "")
    for bad, good in MOJIBAKE.items():
        text = text.replace(bad, good)
    text = unicodedata.normalize("NFKC", text)
    text = HTML_RE.sub(" ", text)
    text = CITATION_RE.sub(" ", text)
    text = CONTROL_RE.sub(" ", text)
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = WS_RE.sub(" ", text)
    text = "\n".join(line.strip() for line in text.split("\n"))
    text = MULTINL_RE.sub("\n\n", text)
    return text.strip()


def content_sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def row_sort_key(text: str) -> str:
    """Deterministic ordering key used to cap rows reproducibly."""
    return content_sha256(text)


def _as_list(value):
    if isinstance(value, list):
        return value
    if isinstance(value, str):
        return [value]
    return []


def render_medqa(row: dict) -> str:
    parts = [str(row.get("question", "")).strip()]
    options = row.get("options")
    if isinstance(options, dict):
        for key in sorted(options):
            parts.append(f"{key}. {options[key]}")
    elif options:
        parts.append(str(options))
    answer = str(row.get("answer", "")).strip()
    if answer:
        parts.append(f"Answer: {answer}")
    return "\n".join(p for p in parts if p)


def render_pubmedqa(row: dict) -> str:
    parts = [str(row.get("question", "")).strip()]
    context = row.get("context")
    if isinstance(context, dict):
        contexts = _as_list(context.get("contexts"))
        labels = _as_list(context.get("labels"))
        for i, ctx in enumerate(contexts):
            label = labels[i] if i < len(labels) else ""
            parts.append(f"{label}: {ctx}" if label else str(ctx))
    elif context:
        parts.append(str(context))
    long_answer = str(row.get("long_answer", "")).strip()
    if long_answer:
        parts.append(long_answer)
    decision = str(row.get("final_decision", "")).strip()
    if decision:
        parts.append(f"Final decision: {decision}")
    return "\n".join(p for p in parts if p)


RENDERERS = {
    "medqa_usmle": render_medqa,
    "pubmedqa_labeled": render_pubmedqa,
    "pubmedqa_artificial": render_pubmedqa,
}


def iter_source_rows(key: str, spec: dict, max_rows: int, counter: dict):
    """Stream a source dataset and yield normalized (text, source_label) rows.

    Each source is capped independently at ``max_rows`` so a small cap still
    draws from every source rather than exhausting the budget on the first one.
    """
    from datasets import load_dataset

    render = RENDERERS[key]
    per_source = 0
    for split in spec["splits"]:
        if per_source >= max_rows:
            break
        try:
            if spec["config"]:
                stream = load_dataset(spec["dataset_id"], spec["config"], split=split, streaming=True)
            else:
                stream = load_dataset(spec["dataset_id"], split=split, streaming=True)
        except Exception as exc:  # noqa: BLE001 - report and continue
            print(f"[warn] {key}/{split}: could not load ({type(exc).__name__}: {exc})", file=sys.stderr)
            continue
        source_label = spec["dataset_id"] if not spec["config"] else f"{spec['dataset_id']}:{spec['config']}"
        for row in stream:
            counter["seen"] += 1
            if per_source >= max_rows:
                break
            text = normalize(render(row))
            if len(text) < MIN_CHARS:
                counter["dropped_short"] += 1
                continue
            per_source += 1
            yield text, source_label


def build_corpus(max_rows: int, out_dir: Path) -> dict:
    out_dir.mkdir(parents=True, exist_ok=True)
    counter = {"seen": 0, "kept": 0, "dropped_short": 0, "dropped_dup": 0}
    per_source = {}
    candidates = []
    seen_hashes = set()

    for key, spec in SOURCES.items():
        kept = 0
        for text, source_label in iter_source_rows(key, spec, max_rows, counter):
            h = content_sha256(text)
            if h in seen_hashes:
                counter["dropped_dup"] += 1
                continue
            seen_hashes.add(h)
            candidates.append((h, text, source_label))
            kept += 1
            counter["kept"] += 1
        per_source[key] = kept
        print(f"[ingest] {key}: kept {kept} rows")

    # Deterministic cap: sort by content hash, then take the first max_rows.
    candidates.sort(key=lambda item: item[0])
    if len(candidates) > max_rows:
        candidates = candidates[:max_rows]

    gz_path = out_dir / "train.jsonl.gz"
    total_tokens = 0
    # mtime=0 keeps the gzip container byte-identical across re-runs.
    with open(gz_path, "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as gz:
        with io.TextIOWrapper(gz, encoding="utf-8") as fh:
            for h, text, source_label in candidates:
                fh.write(json.dumps({"text": text, "source": source_label}, ensure_ascii=False) + "\n")
                total_tokens += len(text.split())

    retrieved_at = datetime.now(timezone.utc).isoformat()
    try:
        output_ref = str(gz_path.relative_to(ROOT))
    except ValueError:
        output_ref = str(gz_path)
    manifest = {
        "version": VERSION,
        "repo": TARGET_REPO,
        "retrieved_at": retrieved_at,
        "processing_version": VERSION,
        "record_count": len(candidates),
        "estimated_tokens": total_tokens,
        "output": output_ref,
        "min_chars": MIN_CHARS,
        "max_rows": max_rows,
        "seen_rows": counter["seen"],
        "dropped_short": counter["dropped_short"],
        "dropped_duplicate": counter["dropped_dup"],
        "sources": {
            key: {
                "dataset_id": spec["dataset_id"],
                "config": spec["config"],
                "splits": spec["splits"],
                "license": spec["license"],
                "attribution": spec["attribution"],
                "rows_kept": per_source.get(key, 0),
            }
            for key, spec in SOURCES.items()
        },
        "omitted_sources": {
            "wikiproject_medicine": "No unambiguous, licence-clear Hub extract found; omitted.",
        },
    }
    return manifest


def fetch_licenses(manifest: dict) -> None:
    """Best-effort: fill licenses from Hub metadata; keep the fallback otherwise."""
    try:
        from huggingface_hub import HfApi

        api = HfApi()
        cache = {}
        for entry in manifest["sources"].values():
            did = entry["dataset_id"]
            if did not in cache:
                try:
                    info = api.dataset_info(did)
                    cache[did] = (info.cardData or {}).get("license", "unknown")
                except Exception as exc:  # noqa: BLE001
                    cache[did] = f"unknown ({type(exc).__name__})"
            if entry["license"] == "verify-on-hub":
                entry["license"] = cache[did]
    except Exception:  # noqa: BLE001 - metadata is best-effort
        pass


def render_card(manifest: dict) -> str:
    lines = [
        "---",
        "license: other",
        "task_categories:",
        "- text-generation",
        "language:",
        "- en",
        "---",
        "",
        "# MEDIA Medical Corpus",
        "",
        f"`{manifest['version']}` — aggregated, normalized open medical text for MEDIA",
        "foundation-model pretraining. English-only for this version; Arabic is planned",
        "as a future layer.",
        "",
        "## Scope",
        "Anatomy, physiology, biochemistry, pathology, pharmacology, microbiology, and",
        "hematology. Rows follow the minimal schema `{\"text\", \"source\"}` where `source`",
        "is the originating Hugging Face dataset id.",
        "",
        "## Sources",
        "",
        "| key | dataset | config | license | attribution | rows kept |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for key, entry in manifest["sources"].items():
        lines.append(
            f"| {key} | {entry['dataset_id']} | {entry['config'] or '-'} | "
            f"{entry['license']} | {entry['attribution']} | {entry['rows_kept']} |"
        )
    lines += [
        "",
        "## Omitted sources",
        "",
    ]
    for key, reason in manifest.get("omitted_sources", {}).items():
        lines.append(f"- {key}: {reason}")
    lines += [
        "",
        "## Normalization",
        "HTML entity decode, mojibake repair, Unicode NFKC, HTML tag stripping,",
        "citation-bracket removal (`[1]`, `[12]`), control-character removal,",
        "whitespace/line cleanup, minimum length filter (>= 100 chars), and exact",
        "duplicate removal. Rows are capped deterministically by content hash.",
        "",
        "## Intended use",
        "Pretraining the MEDIA 100M foundation model. Not for clinical decision-making.",
        "",
        "## Statistics",
        f"- records: {manifest['record_count']}",
        f"- estimated tokens: {manifest['estimated_tokens']}",
        f"- retrieved: {manifest['retrieved_at']}",
        "",
        "## Attribution",
        "Each source dataset retains its own license and attribution terms; see the",
        "table above. Consumers must comply with the upstream licenses.",
        "",
    ]
    return "\n".join(lines)


def publish(manifest: dict, out_dir: Path) -> tuple[bool, str]:
    token = os.environ.get("HF_TOKEN")
    if not token:
        return False, "HF_TOKEN is not set; skipping upload (ingestion succeeded)."

    from huggingface_hub import HfApi

    api = HfApi(token=token)
    try:
        api.whoami()
    except Exception as exc:  # noqa: BLE001
        return False, f"HF_TOKEN is invalid ({type(exc).__name__}); skipping upload: {exc}"

    api.create_repo(TARGET_REPO, repo_type="dataset", private=False, exist_ok=True)
    api.upload_file(
        path_or_fileobj=str(out_dir / "train.jsonl.gz"),
        path_in_repo="train.jsonl.gz",
        repo_id=TARGET_REPO,
        repo_type="dataset",
        commit_message=f"Add {VERSION} medical corpus",
    )
    card = render_card(manifest)
    (out_dir / "README.md").write_text(card, encoding="utf-8")
    api.upload_file(
        path_or_fileobj=str(out_dir / "README.md"),
        path_in_repo="README.md",
        repo_id=TARGET_REPO,
        repo_type="dataset",
        commit_message=f"Add dataset card for {VERSION}",
    )
    return True, f"Published to https://huggingface.co/datasets/{TARGET_REPO}"


def verify(repo_id: str) -> dict | None:
    try:
        from datasets import load_dataset

        stream = load_dataset(repo_id, split="train", streaming=True)
        samples = 0
        tokens = 0
        for row in stream:
            samples += 1
            tokens += len(str(row.get("text", "")).split())
            if samples >= 1000:
                break
        return {"sampled": samples, "sampled_tokens": tokens}
    except Exception as exc:  # noqa: BLE001
        print(f"[warn] verification failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        return None


def main() -> int:
    parser = argparse.ArgumentParser(description="Build and publish the MEDIA medical corpus.")
    parser.add_argument("--output", default=str(DEFAULT_OUT), help="local working directory")
    parser.add_argument("--max-rows", type=int, default=DEFAULT_MAX_ROWS)
    parser.add_argument("--skip-upload", action="store_true", help="build locally, do not publish")
    args = parser.parse_args()

    out_dir = Path(args.output)
    manifest = build_corpus(args.max_rows, out_dir)
    fetch_licenses(manifest)
    (out_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (out_dir / "README.md").write_text(render_card(manifest), encoding="utf-8")

    print(json.dumps({"record_count": manifest["record_count"],
                      "estimated_tokens": manifest["estimated_tokens"],
                      "output": manifest["output"]}, ensure_ascii=False, indent=2))

    if args.skip_upload:
        print("Upload skipped (--skip-upload).")
        return 0

    ok, message = publish(manifest, out_dir)
    print(message)
    if not ok:
        return 0  # ingestion succeeded; publication blocked by credentials

    result = verify(TARGET_REPO)
    if result is not None:
        print(f"Verification: streamed {result['sampled']} samples "
              f"({result['sampled_tokens']} tokens in sample).")
    print(f"URL: https://huggingface.co/datasets/{TARGET_REPO}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
