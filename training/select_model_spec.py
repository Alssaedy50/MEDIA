#!/usr/bin/env python3
"""Select and materialize the MEDIA 100M model foundation from tokenizer benchmarks."""
from __future__ import annotations
import argparse
import json
from pathlib import Path

def select(data: dict) -> dict:
    results = data.get("results") or []
    if not results:
        raise ValueError("benchmark contains no results")
    valid = [r for r in results if r.get("unknown_token_count") == 0 and r.get("token_count_including_specials", 0) > 0]
    if not valid:
        raise ValueError("no valid tokenizer candidate with zero unknown tokens")
    best_tokens = min(r["token_count_including_specials"] for r in valid)
    threshold = best_tokens * 1.02
    eligible = [r for r in valid if r["token_count_including_specials"] <= threshold]
    eligible.sort(key=lambda r: (r["actual_vocab_size"], r["requested_vocab_size"]))
    chosen = eligible[0]
    arch = chosen["architecture_near_100m"]
    return {
        "version": "media-100m-foundation-v1",
        "status": "candidate_pending_freeze",
        "selection_policy": {
            "unknown_tokens_required": 0,
            "compression_tolerance": "within 2% of minimum token count",
            "tie_breaker": "smallest actual vocabulary",
            "model_target_parameters": 100000000,
            "max_sequence_length": 256,
            "weight_tying": True,
        },
        "benchmark": {
            "source_version": data.get("version"),
            "corpus_paths": data.get("corpus_paths"),
            "candidate_count": len(results),
        },
        "selected_tokenizer": {
            "requested_vocab_size": chosen["requested_vocab_size"],
            "actual_vocab_size": chosen["actual_vocab_size"],
            "merge_count": chosen["merge_count"],
            "token_count_including_specials": chosen["token_count_including_specials"],
            "mean_tokens": chosen["mean_tokens"],
            "p95_tokens": chosen["p95_tokens"],
            "max_tokens": chosen["max_tokens"],
            "compression_ratio_tokens_per_pretoken": chosen["compression_ratio_tokens_per_pretoken"],
        },
        "transformer": {
            "vocab_size": chosen["actual_vocab_size"],
            "max_seq_len": 256,
            "d_model": arch["d_model"],
            "n_heads": arch["n_heads"],
            "n_layers": arch["n_layers"],
            "d_ff": arch["d_ff"],
            "parameter_count": chosen["parameter_count_near_100m"],
            "weight_tying": True,
        },
        "eligible_candidates": [
            {"requested_vocab_size": r["requested_vocab_size"], "actual_vocab_size": r["actual_vocab_size"],
             "token_count": r["token_count_including_specials"], "parameter_count": r["parameter_count_near_100m"]}
            for r in eligible
        ],
        "all_candidates": [
            {"requested_vocab_size": r["requested_vocab_size"], "actual_vocab_size": r["actual_vocab_size"],
             "token_count": r["token_count_including_specials"], "unknown_tokens": r["unknown_token_count"]}
            for r in results
        ],
    }

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("benchmark")
    parser.add_argument("--output", default="model/foundation_spec.json")
    args = parser.parse_args()
    spec = select(json.loads(Path(args.benchmark).read_text(encoding="utf-8")))
    Path(args.output).write_text(json.dumps(spec, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(spec, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
