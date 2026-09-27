from __future__ import annotations
import json
from pathlib import Path
import torch
from torch.utils.data import Dataset
from training.tokenizer import BPETokenizer

class JsonlCausalDataset(Dataset):
    def __init__(self,path,tokenizer,max_seq_len=256):
        self.path=Path(path); self.tokenizer=tokenizer; self.max_seq_len=max_seq_len
        self.rows=[json.loads(x) for x in self.path.read_text(encoding="utf-8").splitlines() if x.strip()]
    def __len__(self): return len(self.rows)
    def __getitem__(self,index):
        row=self.rows[index]
        p=self.tokenizer.encode(row["input"],add_special_tokens=True)
        t=self.tokenizer.encode(row["target"],add_special_tokens=False)+[self.tokenizer.eos_id]
        ids=(p+t)[:self.max_seq_len+1]
        if len(ids)<2: ids += [self.tokenizer.eos_id]
        return {"input_ids":torch.tensor(ids[:-1],dtype=torch.long),"targets":torch.tensor(ids[1:],dtype=torch.long),"id":row["id"],"task":row.get("task","")}

def collate_causal(batch,pad_id):
    n=max(x["input_ids"].numel() for x in batch); inputs=torch.full((len(batch),n),pad_id,dtype=torch.long); targets=inputs.clone()
    for i,item in enumerate(batch):
        k=item["input_ids"].numel(); inputs[i,:k]=item["input_ids"]; targets[i,:k]=item["targets"]
    return {"input_ids":inputs,"targets":targets}
