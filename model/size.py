from __future__ import annotations
from dataclasses import dataclass, asdict
from itertools import product

@dataclass(frozen=True)
class Architecture:
    vocab_size: int
    max_seq_len: int
    d_model: int
    n_heads: int
    n_layers: int
    d_ff: int
    parameter_count: int
    tied_embeddings: bool = True

def parameter_count(vocab_size: int, max_seq_len: int, d_model: int, n_layers: int, d_ff: int, tied_embeddings: bool = True) -> int:
    if not all(x > 0 for x in (vocab_size, max_seq_len, d_model, n_layers, d_ff)):
        raise ValueError("all dimensions must be positive")
    # Token + positional embeddings, decoder blocks, final LayerNorm.
    embedding = vocab_size * d_model + max_seq_len * d_model
    block = 4 * d_model * d_model + 2 * d_ff * d_model + 4 * d_model
    output = 0 if tied_embeddings else vocab_size * d_model
    return embedding + n_layers * block + 2 * d_model + output

def choose_heads(d_model: int, candidates=(8, 16, 12, 10, 6, 4, 2)) -> int:
    for heads in candidates:
        if d_model % heads == 0:
            return heads
    raise ValueError("no compatible attention-head count")

def find_near_target(vocab_size: int, max_seq_len: int = 256, target: int = 100_000_000,
                     d_models=(256, 320, 384, 448, 512, 576, 640, 704, 768, 832, 896, 960),
                     layer_range=range(4, 15), ff_multipliers=(3, 4),
                     max_results: int = 10) -> list[Architecture]:
    candidates = []
    for d_model, n_layers, mult in product(d_models, layer_range, ff_multipliers):
        d_ff = d_model * mult
        heads = choose_heads(d_model)
        count = parameter_count(vocab_size, max_seq_len, d_model, n_layers, d_ff)
        candidates.append(Architecture(vocab_size, max_seq_len, d_model, heads, n_layers, d_ff, count))
    candidates.sort(key=lambda a: (abs(a.parameter_count - target), a.parameter_count, a.d_model, a.n_layers))
    return candidates[:max_results]

def summarize(architecture: Architecture) -> dict:
    data = asdict(architecture)
    data["distance_from_100m"] = architecture.parameter_count - 100_000_000
    data["distance_percent"] = round(abs(data["distance_from_100m"]) / 100_000_000 * 100, 4)
    return data

def main() -> None:
    import argparse, json
    p = argparse.ArgumentParser(description="Search exact MEDIA Transformer parameter counts.")
    p.add_argument("--vocab-size", type=int, required=True)
    p.add_argument("--max-seq-len", type=int, default=256)
    p.add_argument("--target", type=int, default=100_000_000)
    p.add_argument("--top-k", type=int, default=10)
    a = p.parse_args()
    print(json.dumps([summarize(x) for x in find_near_target(a.vocab_size, a.max_seq_len, a.target, max_results=a.top_k)], indent=2))

if __name__ == "__main__":
    main()
