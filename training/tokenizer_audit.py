from __future__ import annotations
import json
from collections import Counter
from pathlib import Path
from statistics import mean

try:
    from .tokenizer import BPETokenizer, normalize, pretokenize
except ImportError:  # Support direct script execution from repository root.
    from tokenizer import BPETokenizer, normalize, pretokenize

def iter_examples(paths):
    for path in paths:
        for line in Path(path).read_text(encoding="utf-8").splitlines():
            if line.strip():
                yield json.loads(line)

def audit(paths, tokenizer_path):
    tokenizer = BPETokenizer.load(tokenizer_path)
    rows = list(iter_examples(paths))
    encoded = []
    languages = Counter()
    word_stats = []
    for row in rows:
        for field in ("input", "target"):
            text = str(row.get(field, ""))
            ids = tokenizer.encode(text)
            encoded.append(len(ids))
            arabic = sum(1 for token in pretokenize(normalize(text)) if any("؀" <= ch <= "ۿ" for ch in token))
            english = sum(1 for token in pretokenize(normalize(text)) if any(("a" <= ch.lower() <= "z") for ch in token))
            languages["ar" if arabic and not english else "en" if english and not arabic else "mixed"] += 1
            words = pretokenize(text)
            word_stats.append((len(words), len(ids)))
    token_total = sum(encoded)
    word_total = sum(x[0] for x in word_stats)
    return {
        "examples": len(rows),
        "texts": len(encoded),
        "token_count_including_specials": token_total,
        "mean_tokens": round(mean(encoded), 3) if encoded else 0,
        "max_tokens": max(encoded, default=0),
        "p50_tokens": _percentile(encoded, 0.50),
        "p90_tokens": _percentile(encoded, 0.90),
        "p95_tokens": _percentile(encoded, 0.95),
        "p99_tokens": _percentile(encoded, 0.99),
        "mean_pretokens": round(word_total / len(word_stats), 3) if word_stats else 0,
        "compression_ratio_tokens_per_pretoken": round(token_total / word_total, 4) if word_total else 0,
        "languages": dict(sorted(languages.items())),
        "vocab_size": len(tokenizer.token_to_id),
        "unknown_token_count": sum(ids.count(tokenizer.unk_id) for row in rows for field in ("input", "target") for ids in [tokenizer.encode(str(row.get(field, "")))]),
    }

def _percentile(values, q):
    if not values:
        return 0
    ordered = sorted(values)
    index = min(len(ordered) - 1, int(q * len(ordered)))
    return ordered[index]

def main():
    import argparse
    parser = argparse.ArgumentParser(description="Audit exact token usage of a generated MEDIA corpus.")
    parser.add_argument("jsonl", nargs="+")
    parser.add_argument("--tokenizer", required=True)
    parser.add_argument("--output")
    args = parser.parse_args()
    result = audit(args.jsonl, args.tokenizer)
    payload = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    print(payload, end="")
    if args.output:
        Path(args.output).write_text(payload, encoding="utf-8")

if __name__ == "__main__":
    main()
