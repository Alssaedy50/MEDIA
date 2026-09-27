from __future__ import annotations

import argparse
import json
import math
import random
from pathlib import Path
from typing import Any

import torch
from torch.utils.data import DataLoader

from model.config import TransformerConfig
from model.data import JsonlCausalDataset, collate_causal
from model.transformer import MediaTransformerLM
from training.tokenizer import BPETokenizer


def set_seed(seed: int) -> None:
    random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


def load_foundation(path: str | Path) -> TransformerConfig:
    return TransformerConfig.from_foundation_spec(path)


def load_tokenizer(path: str | Path) -> BPETokenizer:
    return BPETokenizer.load(path)


def build_model(foundation_path: str | Path, tokenizer: BPETokenizer) -> MediaTransformerLM:
    config = load_foundation(foundation_path)
    if config.vocab_size != tokenizer.vocab_size:
        raise ValueError(
            f"tokenizer/model vocabulary mismatch: tokenizer={tokenizer.vocab_size}, model={config.vocab_size}"
        )
    return MediaTransformerLM(config)


def checkpoint_payload(
    model: MediaTransformerLM,
    optimizer: torch.optim.Optimizer,
    scheduler: torch.optim.lr_scheduler.LRScheduler | None,
    scaler: Any,
    epoch: int,
    global_step: int,
    best_val_loss: float,
    config: dict[str, Any],
) -> dict[str, Any]:
    payload = {
        "format_version": "media-training-checkpoint-v1",
        "model_state_dict": model.state_dict(),
        "optimizer_state_dict": optimizer.state_dict(),
        "epoch": epoch,
        "global_step": global_step,
        "best_val_loss": best_val_loss,
        "training_config": config,
        "architecture": model.architecture(),
    }
    if scheduler is not None:
        payload["scheduler_state_dict"] = scheduler.state_dict()
    if scaler is not None and scaler.is_enabled():
        payload["scaler_state_dict"] = scaler.state_dict()
    return payload


def save_checkpoint(path: str | Path, payload: dict[str, Any]) -> None:
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    torch.save(payload, tmp)
    tmp.replace(path)


def load_checkpoint(
    path: str | Path,
    model: MediaTransformerLM,
    optimizer: torch.optim.Optimizer,
    scheduler: torch.optim.lr_scheduler.LRScheduler | None = None,
    scaler: Any = None,
    map_location: str | torch.device = "cpu",
) -> tuple[int, int, float, dict[str, Any]]:
    checkpoint = torch.load(path, map_location=map_location, weights_only=False)
    if checkpoint.get("format_version") != "media-training-checkpoint-v1":
        raise ValueError("unsupported MEDIA training checkpoint format")
    model.load_state_dict(checkpoint["model_state_dict"])
    optimizer.load_state_dict(checkpoint["optimizer_state_dict"])
    if scheduler is not None and "scheduler_state_dict" in checkpoint:
        scheduler.load_state_dict(checkpoint["scheduler_state_dict"])
    if scaler is not None and "scaler_state_dict" in checkpoint and scaler.is_enabled():
        scaler.load_state_dict(checkpoint["scaler_state_dict"])
    return (
        int(checkpoint.get("epoch", 0)),
        int(checkpoint.get("global_step", 0)),
        float(checkpoint.get("best_val_loss", float("inf"))),
        checkpoint.get("training_config", {}),
    )


@torch.no_grad()
def evaluate(model: MediaTransformerLM, loader: DataLoader, device: torch.device) -> float:
    model.eval()
    losses = []
    for batch in loader:
        inputs = batch["input_ids"].to(device)
        targets = batch["targets"].to(device)
        _, loss = model(inputs, targets)
        losses.append(float(loss.detach().cpu()))
    if not losses:
        return float("inf")
    return sum(losses) / len(losses)


def train(args: argparse.Namespace) -> dict[str, Any]:
    set_seed(args.seed)
    device = torch.device(args.device or ("cuda" if torch.cuda.is_available() else "cpu"))
    tokenizer = load_tokenizer(args.tokenizer)
    model = build_model(args.foundation, tokenizer).to(device)

    train_ds = JsonlCausalDataset(args.train, tokenizer, model.config.max_seq_len)
    val_ds = JsonlCausalDataset(args.validation, tokenizer, model.config.max_seq_len)
    collate = lambda batch: collate_causal(batch, tokenizer.pad_id)
    train_loader = DataLoader(train_ds, batch_size=args.batch_size, shuffle=True, collate_fn=collate)
    val_loader = DataLoader(val_ds, batch_size=args.batch_size, shuffle=False, collate_fn=collate)

    optimizer = torch.optim.AdamW(model.parameters(), lr=args.learning_rate, weight_decay=args.weight_decay)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=max(1, args.max_steps))
    amp_enabled = bool(args.amp and device.type == "cuda")
    amp_dtype = torch.float16 if args.amp_dtype == "float16" else torch.bfloat16
    scaler = torch.amp.GradScaler("cuda", enabled=amp_enabled)

    start_epoch = 0
    global_step = 0
    best_val_loss = float("inf")
    if args.resume:
        start_epoch, global_step, best_val_loss, _ = load_checkpoint(
            args.resume, model, optimizer, scheduler, scaler, map_location=device
        )

    run_config = vars(args).copy()
    run_config["device"] = str(device)
    run_config["model_parameters"] = model.parameter_count()
    run_config["tokenizer_vocab_size"] = tokenizer.vocab_size

    model.train()
    while global_step < args.max_steps:
        for epoch in range(start_epoch, args.epochs):
            for batch in train_loader:
                if global_step >= args.max_steps:
                    break
                inputs = batch["input_ids"].to(device)
                targets = batch["targets"].to(device)
                optimizer.zero_grad(set_to_none=True)
                with torch.autocast(device_type=device.type, dtype=amp_dtype, enabled=amp_enabled):
                    _, loss = model(inputs, targets)
                if not torch.isfinite(loss):
                    raise FloatingPointError(f"non-finite training loss at step {global_step}: {loss.item()}")
                scaler.scale(loss).backward()
                scaler.unscale_(optimizer)
                grad_norm = torch.nn.utils.clip_grad_norm_(model.parameters(), args.max_grad_norm)
                if not torch.isfinite(grad_norm):
                    raise FloatingPointError(f"non-finite gradient norm at step {global_step}")
                scaler.step(optimizer)
                scaler.update()
                scheduler.step()
                global_step += 1

                if global_step % args.eval_every == 0 or global_step == args.max_steps:
                    val_loss = evaluate(model, val_loader, device)
                    model.train()
                    if val_loss < best_val_loss:
                        best_val_loss = val_loss
                        save_checkpoint(
                            args.best_checkpoint,
                            checkpoint_payload(
                                model, optimizer, scheduler, scaler, epoch + 1, global_step,
                                best_val_loss, run_config
                            ),
                        )
                if global_step % args.save_every == 0 or global_step == args.max_steps:
                    save_checkpoint(
                        args.checkpoint,
                        checkpoint_payload(
                            model, optimizer, scheduler, scaler, epoch + 1, global_step,
                            best_val_loss, run_config
                        ),
                    )
            start_epoch = epoch + 1

    return {
        "status": "completed",
        "device": str(device),
        "global_step": global_step,
        "best_val_loss": best_val_loss,
        "parameter_count": model.parameter_count(),
        "checkpoint": str(args.checkpoint),
        "best_checkpoint": str(args.best_checkpoint),
        "perplexity": math.exp(best_val_loss) if math.isfinite(best_val_loss) else None,
    }


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="Train MEDIA causal language model.")
    p.add_argument("--foundation", default="model/foundation_spec.json")
    p.add_argument("--tokenizer", required=True)
    p.add_argument("--train", required=True)
    p.add_argument("--validation", required=True)
    p.add_argument("--checkpoint", default="checkpoints/media-lm-last.pt")
    p.add_argument("--best-checkpoint", default="checkpoints/media-lm-best.pt")
    p.add_argument("--resume")
    p.add_argument("--device")
    p.add_argument("--epochs", type=int, default=1)
    p.add_argument("--max-steps", type=int, default=1000)
    p.add_argument("--batch-size", type=int, default=2)
    p.add_argument("--learning-rate", type=float, default=3e-4)
    p.add_argument("--weight-decay", type=float, default=0.1)
    p.add_argument("--max-grad-norm", type=float, default=1.0)
    p.add_argument("--eval-every", type=int, default=100)
    p.add_argument("--save-every", type=int, default=100)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--amp", action="store_true")
    p.add_argument("--amp-dtype", choices=("float16", "bfloat16"), default="float16")
    return p


if __name__ == "__main__":
    result = train(build_parser().parse_args())
    print(json.dumps(result, indent=2))
