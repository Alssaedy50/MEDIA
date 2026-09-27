from __future__ import annotations
from dataclasses import dataclass, asdict

@dataclass(frozen=True)
class TransformerConfig:
    vocab_size: int = 4096
    max_seq_len: int = 256
    d_model: int = 256
    n_heads: int = 8
    n_layers: int = 6
    d_ff: int = 1024
    dropout: float = 0.1
    pad_id: int = 0
    bos_id: int = 1
    eos_id: int = 2

    def __post_init__(self):
        if self.d_model % self.n_heads: raise ValueError("d_model must be divisible by n_heads")
        if min(self.vocab_size, self.max_seq_len, self.d_model, self.n_heads, self.n_layers, self.d_ff) <= 0:
            raise ValueError("model dimensions must be positive")

    @property
    def head_dim(self): return self.d_model // self.n_heads
    def to_dict(self): return asdict(self)
