"""MEDIA model runtime boundary.

Loads the frozen foundation configuration and, when a compatible checkpoint is
available, exposes local generation. It never fabricates a model checkpoint:
without weights the runtime reports "weights_unavailable" so the application
can continue using retrieval/evidence mode.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
FOUNDATION_PATH = ROOT / "model" / "foundation_spec.json"
TOKENIZER_PATH = ROOT / "datasets" / "hematology" / "tokenizer.json"

try:
    import torch
except ImportError:  # pragma: no cover
    torch = None

try:
    from model.config import TransformerConfig
    from model.transformer import MediaTransformerLM
    from training.tokenizer import BPETokenizer
except ImportError:  # pragma: no cover
    TransformerConfig = None
    MediaTransformerLM = None
    BPETokenizer = None


class ModelRuntime:
    """Lazy local-model runtime for Android/API integration."""

    def __init__(
        self,
        foundation_path: Path = FOUNDATION_PATH,
        tokenizer_path: Path = TOKENIZER_PATH,
        checkpoint_path: Path | None = None,
        device: str = "cpu",
    ) -> None:
        self.foundation_path = Path(foundation_path)
        self.tokenizer_path = Path(tokenizer_path)
        self.checkpoint_path = Path(checkpoint_path) if checkpoint_path else None
        self.device = device
        self.model = None
        self.tokenizer = None
        self._status = "not_loaded"

    def foundation(self) -> dict[str, Any]:
        return json.loads(self.foundation_path.read_text(encoding="utf-8"))

    def status(self) -> dict[str, Any]:
        spec = self.foundation()
        return {
            "runtime": "MEDIA local Transformer runtime",
            "foundation_status": spec.get("status"),
            "foundation_version": spec.get("version"),
            "weights_available": bool(self.checkpoint_path and self.checkpoint_path.exists()),
            "checkpoint": str(self.checkpoint_path) if self.checkpoint_path else None,
            "state": self._status,
            "architecture": spec.get("transformer", {}),
        }

    def load(self) -> dict[str, Any]:
        if torch is None or MediaTransformerLM is None or TransformerConfig is None:
            self._status = "runtime_dependencies_unavailable"
            return self.status()
        if not self.checkpoint_path or not self.checkpoint_path.exists():
            self._status = "weights_unavailable"
            return self.status()
        if not self.tokenizer_path.exists():
            self._status = "tokenizer_unavailable"
            return self.status()

        spec = self.foundation()
        if spec.get("status") != "frozen":
            self._status = "foundation_not_frozen"
            return self.status()

        config = TransformerConfig.from_foundation_spec(self.foundation_path)
        self.tokenizer = BPETokenizer.load(self.tokenizer_path)
        if len(self.tokenizer.config["vocab"]) != config.vocab_size:
            self._status = "tokenizer_foundation_mismatch"
            return self.status()

        self.model = MediaTransformerLM(config)
        checkpoint = torch.load(self.checkpoint_path, map_location=self.device, weights_only=False)
        state = checkpoint.get("model", checkpoint.get("model_state_dict", checkpoint))
        self.model.load_state_dict(state, strict=True)
        self.model.to(self.device)
        self.model.eval()
        self._status = "ready"
        return self.status()

    @property
    def ready(self) -> bool:
        return self.model is not None and self.tokenizer is not None and self._status == "ready"

    def generate(self, prompt: str, max_new_tokens: int = 96, temperature: float = 0.0) -> str:
        if not self.ready:
            raise RuntimeError("MEDIA local model weights are not available.")
        ids = self.tokenizer.encode(prompt, add_special_tokens=True)
        input_ids = torch.tensor([ids], dtype=torch.long, device=self.device)
        output = self.model.generate(
            input_ids,
            max_new_tokens=max_new_tokens,
            temperature=temperature,
            eos_id=self.tokenizer.eos_id,
        )[0].tolist()
        return self.tokenizer.decode(output)


def runtime_status() -> dict[str, Any]:
    return ModelRuntime().status()
