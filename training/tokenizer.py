#!/usr/bin/env python3
"""Deterministic bilingual subword tokenizer for the MEDIA corpus."""
from __future__ import annotations
import json, re, unicodedata
from collections import Counter
from pathlib import Path
from typing import Iterable

SPECIAL_TOKENS = ["<pad>", "<unk>", "<bos>", "<eos>"]
TOKEN_RE = re.compile(r"[A-Za-z][A-Za-z0-9]*|[\u0600-\u06FF]+|[0-9]+|[^\s]", re.UNICODE)
WORD_END = "</w>"

def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKC", str(text))
    return " ".join(text.strip().split())

def pretokenize(text: str) -> list[str]:
    return TOKEN_RE.findall(normalize(text))

def _word_symbols(word: str) -> tuple[str, ...]:
    return tuple(list(word) + [WORD_END])

def _merge_pair(symbols: tuple[str, ...], pair: tuple[str, str]) -> tuple[str, ...]:
    merged, i = [], 0
    while i < len(symbols):
        if i + 1 < len(symbols) and (symbols[i], symbols[i + 1]) == pair:
            merged.append(symbols[i] + symbols[i + 1])
            i += 2
        else:
            merged.append(symbols[i])
            i += 1
    return tuple(merged)

def train_vocab(texts: Iterable[str], vocab_size: int = 4096, min_frequency: int = 2) -> dict:
    if vocab_size < len(SPECIAL_TOKENS) + 32:
        raise ValueError("vocab_size is too small")
    word_counts: Counter[str] = Counter()
    for text in texts:
        word_counts.update(pretokenize(text))
    sequences = {word: _word_symbols(word) for word in sorted(word_counts)}
    vocab = set(SPECIAL_TOKENS)
    for symbols in sequences.values():
        vocab.update(symbols)
    merges = []
    while len(vocab) < vocab_size:
        pair_counts: Counter[tuple[str, str]] = Counter()
        for word, symbols in sequences.items():
            for i in range(len(symbols) - 1):
                pair_counts[(symbols[i], symbols[i + 1])] += word_counts[word]
        candidates = [(count, pair) for pair, count in pair_counts.items() if count >= min_frequency]
        if not candidates:
            break
        _, best = max(candidates, key=lambda item: (item[0], item[1]))
        merges.append(best)
        vocab.add(best[0] + best[1])
        for word in list(sequences):
            sequences[word] = _merge_pair(sequences[word], best)
    return {
        "version": "media-bpe-v1",
        "algorithm": "deterministic-bpe",
        "normalization": "NFKC + whitespace collapse",
        "special_tokens": SPECIAL_TOKENS,
        "word_end": WORD_END,
        "vocab_size": len(vocab),
        "merges": [list(pair) for pair in merges],
        "vocab": sorted(vocab),
    }

class BPETokenizer:
    def __init__(self, config: dict):
        self.config = config
        self.token_to_id = {t: i for i, t in enumerate(config["vocab"])}
        self.id_to_token = {i: t for t, i in self.token_to_id.items()}
        self.unk_id = self.token_to_id["<unk>"]
        self.bos_id = self.token_to_id["<bos>"]
        self.eos_id = self.token_to_id["<eos>"]
        self.merge_rank = {tuple(pair): i for i, pair in enumerate(config["merges"])}

    @property
    def vocab_size(self) -> int:
        """Return the effective vocabulary size represented by this tokenizer."""
        return len(self.token_to_id)

    def _encode_word(self, word: str) -> list[str]:
        symbols = _word_symbols(word)
        while len(symbols) > 1:
            ranked = [
                (self.merge_rank.get((symbols[i], symbols[i + 1]), 10**12),
                 (symbols[i], symbols[i + 1]))
                for i in range(len(symbols) - 1)
            ]
            rank, pair = min(ranked, key=lambda item: (item[0], item[1]))
            if rank == 10**12:
                break
            symbols = _merge_pair(symbols, pair)
        return list(symbols)

    def encode(self, text: str, add_special_tokens: bool = True) -> list[int]:
        ids = [self.bos_id] if add_special_tokens else []
        for word in pretokenize(text):
            ids.extend(self.token_to_id.get(t, self.unk_id) for t in self._encode_word(word))
        if add_special_tokens:
            ids.append(self.eos_id)
        return ids

    def decode(self, ids: Iterable[int]) -> str:
        pieces = [self.id_to_token.get(int(i), "<unk>") for i in ids]
        words, current = [], ""
        for piece in pieces:
            if piece in {"<pad>", "<bos>", "<eos>"}:
                continue
            if piece == "<unk>":
                if current:
                    words.append(current)
                    current = ""
                words.append("<unk>")
                continue
            current += piece.replace(WORD_END, "")
            if piece.endswith(WORD_END):
                words.append(current)
                current = ""
        if current:
            words.append(current)
        return " ".join(words)

    def save(self, path: str | Path) -> None:
        Path(path).write_text(json.dumps(self.config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    @classmethod
    def load(cls, path: str | Path) -> "BPETokenizer":
        return cls(json.loads(Path(path).read_text(encoding="utf-8")))

def train_from_jsonl(paths: Iterable[str | Path], vocab_size: int = 4096) -> dict:
    texts = []
    for path in paths:
        for line in Path(path).read_text(encoding="utf-8").splitlines():
            if line.strip():
                row = json.loads(line)
                texts.extend([row.get("input", ""), row.get("target", "")])
    return train_vocab(texts, vocab_size=vocab_size)

def main() -> None:
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("jsonl", nargs="+")
    parser.add_argument("--vocab-size", type=int, default=4096)
    parser.add_argument("--output", default="datasets/hematology/tokenizer.json")
    args = parser.parse_args()
    config = train_from_jsonl(args.jsonl, vocab_size=args.vocab_size)
    BPETokenizer(config).save(args.output)

if __name__ == "__main__":
    main()
