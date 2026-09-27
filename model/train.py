from __future__ import annotations
import argparse,json,random
from pathlib import Path
import torch
from torch.utils.data import DataLoader
from .config import TransformerConfig
from .data import JsonlCausalDataset,collate_causal
from .transformer import MediaTransformerLM
from training.tokenizer import BPETokenizer

def set_seed(seed): random.seed(seed); torch.manual_seed(seed)
def save_checkpoint(path,model,optimizer,step,loss):
    Path(path).parent.mkdir(parents=True,exist_ok=True)
    torch.save({"format":"media-checkpoint-v1","step":step,"loss":loss,"config":model.config.to_dict(),"model":model.state_dict(),"optimizer":optimizer.state_dict()},path)
def load_checkpoint(path,model,optimizer=None,map_location="cpu"):
    p=torch.load(path,map_location=map_location,weights_only=False); model.load_state_dict(p["model"])
    if optimizer is not None and p.get("optimizer"): optimizer.load_state_dict(p["optimizer"])
    return int(p.get("step",0)),float(p.get("loss",0.0))
def train(args):
    set_seed(args.seed); device=torch.device(args.device); tok=BPETokenizer.load(args.tokenizer)
    cfg=TransformerConfig(vocab_size=len(tok.config["vocab"]),max_seq_len=args.max_seq_len,d_model=args.d_model,n_heads=args.n_heads,n_layers=args.n_layers,d_ff=args.d_ff,dropout=args.dropout,pad_id=tok.token_to_id["<pad>"],bos_id=tok.bos_id,eos_id=tok.eos_id)
    model=MediaTransformerLM(cfg).to(device); opt=torch.optim.AdamW(model.parameters(),lr=args.lr,weight_decay=args.weight_decay)
    ds=JsonlCausalDataset(args.dataset,tok,args.max_seq_len); loader=DataLoader(ds,batch_size=args.batch_size,shuffle=True,collate_fn=lambda b:collate_causal(b,cfg.pad_id))
    step=0; last=0.0
    if args.resume: step,last=load_checkpoint(args.resume,model,opt,device)
    model.train()
    while step<args.steps:
        for batch in loader:
            if step>=args.steps: break
            opt.zero_grad(set_to_none=True); _,loss=model(batch["input_ids"].to(device),batch["targets"].to(device)); loss.backward(); torch.nn.utils.clip_grad_norm_(model.parameters(),args.grad_clip); opt.step(); step+=1; last=float(loss.detach())
            if step%args.log_every==0 or step==1: print(json.dumps({"step":step,"loss":last,"params":model.parameter_count()}))
            if args.checkpoint and step%args.checkpoint_every==0: save_checkpoint(args.checkpoint,model,opt,step,last)
    if args.checkpoint: save_checkpoint(args.checkpoint,model,opt,step,last)
    result={"step":step,"loss":last,"parameter_count":model.parameter_count(),"device":str(device),"checkpoint":args.checkpoint}; print(json.dumps(result)); return result

def main():
    p=argparse.ArgumentParser(); p.add_argument("dataset"); p.add_argument("tokenizer"); p.add_argument("--checkpoint",default="checkpoints/media-tiny.pt"); p.add_argument("--resume"); p.add_argument("--steps",type=int,default=100); p.add_argument("--batch-size",type=int,default=8); p.add_argument("--max-seq-len",type=int,default=256); p.add_argument("--d-model",type=int,default=256); p.add_argument("--n-heads",type=int,default=8); p.add_argument("--n-layers",type=int,default=6); p.add_argument("--d-ff",type=int,default=1024); p.add_argument("--dropout",type=float,default=.1); p.add_argument("--lr",type=float,default=3e-4); p.add_argument("--weight-decay",type=float,default=.1); p.add_argument("--grad-clip",type=float,default=1.0); p.add_argument("--checkpoint-every",type=int,default=100); p.add_argument("--log-every",type=int,default=10); p.add_argument("--seed",type=int,default=42); p.add_argument("--device",default="cpu"); train(p.parse_args())
if __name__=="__main__": main()
