#!/usr/bin/env python3
"""Benchmark MEDIA BPE vocabulary sizes on the generated corpus."""
from __future__ import annotations

import argparse
import json
import time
from pathlib import Path

from model.size import find_near_target
from training.tokenizer import BPETokenizer, train_from_jsonl
from training.tokenizer_audit import audit


def benchmark(paths: list[str], vocab_sizes: list[int], output: str | None = None) -> dict:
    rows = []
    for requested_size in vocab_sizes:
        started = time.perf_counter()
        config = train_from_jsonl(paths, vocab_size=requested_size)
        tokenizer_path = Path(output).with_suffix(f".{requested_size}.json") if output else None
        if tokenizer_path:
            BPETokenizer(config).save(tokenizer_path)
            result = audit(paths, str(tokenizer_path))
        else:
            tmp = Path(".media-tokenizer-benchmark.json")
            BPETokenizer(config).save(tmp)
            result = audit(paths, str(tmp))
            tmp.unlink(missing_ok=True)
        elapsed = time.perf_counter() - started
        arch = find_near_target(
            result["vocab_size"], max_seq_len=256, target=100_000_000, max_results=1
        )[0]
        rows.append({
            "requested_vocab_size": requested_size,
            "actual_vocab_size": result["vocab_size"],
            "merge_count": len(config["merges"]),
            "token_count_including_specials": result["token_count_including_specials"],
            "mean_tokens": result["mean_tokens"],
            "p50_tokens": result["p50_tokens"],
            "p90_tokens": result["p90_tokens"],
            "p95_tokens": result["p95_tokens"],
            "p99_tokens": result["p99_tokens"],
            "max_tokens": result["max_tokens"],
            "mean_pretokens": result["mean_pretokens"],
            "compression_ratio_tokens_per_pretoken": result["compression_ratio_tokens_per_pretoken"],
            "unknown_token_count": result["unknown_token_count"],
            "parameter_count_near_100m": arch.parameter_count,
            "architecture_near_100m": {
                "d_model": arch.d_model,
                "n_heads": arch.n_heads,
                "n_layers": arch.n_layers,
                "d_ff": arch.d_ff,
            },
            "benchmark_seconds": round(elapsed, 3),
        })
    payload = {
        "version": "media-bpe-benchmark-v1",
        "corpus_paths": paths,
        "vocab_sizes_requested": vocab_sizes,
        "results": rows,
    }
    if output:
        Path(output).write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
    return payload


def main() -> None:
    parser = argparse.ArgumentParser(description="Benchmark candidate MEDIA BPE vocabulary sizes.")
    parser.add_argument("jsonl", nargs="+")
    parser.add_argument(
        "--vocab-sizes", nargs="+", type=int,
        default=[4096, 8192, 16384, 32768, 50000],
    )
    parser.add_argument(
        "--output", default="datasets/hematology/tokenizer_benchmark.json"
    )
    args = parser.parse_args()
    payload = benchmark(args.jsonl, args.vocab_sizes, args.output)
    print(json.dumps(payload, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
