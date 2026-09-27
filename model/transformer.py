from __future__ import annotations
import math
import torch
from torch import nn
import torch.nn.functional as F
from .config import TransformerConfig

class CausalSelfAttention(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.qkv=nn.Linear(config.d_model,3*config.d_model,bias=False)
        self.proj=nn.Linear(config.d_model,config.d_model,bias=False)
        self.dropout=nn.Dropout(config.dropout)
        self.n_heads=config.n_heads; self.head_dim=config.head_dim
        self.register_buffer("causal_mask",torch.triu(torch.ones(config.max_seq_len,config.max_seq_len),1).bool(),persistent=False)
    def forward(self,x):
        b,s,c=x.shape
        q,k,v=self.qkv(x).chunk(3,-1)
        q=q.view(b,s,self.n_heads,self.head_dim).transpose(1,2)
        k=k.view(b,s,self.n_heads,self.head_dim).transpose(1,2)
        v=v.view(b,s,self.n_heads,self.head_dim).transpose(1,2)
        a=(q@k.transpose(-2,-1))/math.sqrt(self.head_dim)
        a=a.masked_fill(self.causal_mask[:s,:s],float("-inf"))
        a=self.dropout(F.softmax(a,dim=-1))
        return self.proj((a@v).transpose(1,2).contiguous().view(b,s,c))

class MLP(nn.Module):
    def __init__(self,config):
        super().__init__(); self.fc1=nn.Linear(config.d_model,config.d_ff); self.fc2=nn.Linear(config.d_ff,config.d_model); self.dropout=nn.Dropout(config.dropout)
    def forward(self,x): return self.dropout(self.fc2(F.gelu(self.fc1(x))))

class DecoderBlock(nn.Module):
    def __init__(self,config):
        super().__init__(); self.norm1=nn.LayerNorm(config.d_model); self.attn=CausalSelfAttention(config); self.norm2=nn.LayerNorm(config.d_model); self.mlp=MLP(config)
    def forward(self,x):
        x=x+self.attn(self.norm1(x)); return x+self.mlp(self.norm2(x))

class MediaTransformerLM(nn.Module):
    def __init__(self,config):
        super().__init__(); self.config=config
        self.token_embedding=nn.Embedding(config.vocab_size,config.d_model,padding_idx=config.pad_id)
        self.position_embedding=nn.Embedding(config.max_seq_len,config.d_model)
        self.dropout=nn.Dropout(config.dropout)
        self.blocks=nn.ModuleList([DecoderBlock(config) for _ in range(config.n_layers)])
        self.norm=nn.LayerNorm(config.d_model); self.lm_head=nn.Linear(config.d_model,config.vocab_size,bias=False)
        self.lm_head.weight=self.token_embedding.weight; self.apply(self._init_weights)
        with torch.no_grad(): self.token_embedding.weight[config.pad_id].zero_()
    def _init_weights(self,module):
        if isinstance(module,nn.Linear):
            nn.init.normal_(module.weight,0.0,0.02)
            if module.bias is not None: nn.init.zeros_(module.bias)
        elif isinstance(module,nn.Embedding): nn.init.normal_(module.weight,0.0,0.02)
    def forward(self,input_ids,targets=None):
        if input_ids.ndim!=2: raise ValueError("input_ids must have shape [batch, sequence]")
        b,s=input_ids.shape
        if s>self.config.max_seq_len: raise ValueError("sequence exceeds max_seq_len")
        pos=torch.arange(s,device=input_ids.device).unsqueeze(0)
        x=self.dropout(self.token_embedding(input_ids)+self.position_embedding(pos))
        for block in self.blocks: x=block(x)
        logits=self.lm_head(self.norm(x)); loss=None
        if targets is not None: loss=F.cross_entropy(logits.reshape(-1,logits.size(-1)),targets.reshape(-1),ignore_index=self.config.pad_id)
        return logits,loss
    @torch.no_grad()
    def generate(self,input_ids,max_new_tokens,temperature=0.0,eos_id=None):
        self.eval()
        for _ in range(max_new_tokens):
            logits,_=self(input_ids[:,-self.config.max_seq_len:]); last=logits[:,-1,:]
            if temperature<=0: nxt=last.argmax(-1,keepdim=True)
            else: nxt=torch.multinomial(F.softmax(last/temperature,dim=-1),1)
            input_ids=torch.cat([input_ids,nxt],1)
            if eos_id is not None and bool(torch.all(nxt==eos_id)): break
        return input_ids
    def parameter_count(self): return sum(p.numel() for p in self.parameters())
    def architecture(self): return {**self.config.to_dict(),"parameter_count":self.parameter_count(),"weight_tying":True}
