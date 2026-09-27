from __future__ import annotations
import tempfile,unittest
from pathlib import Path
import torch
from model.config import TransformerConfig
from model.transformer import MediaTransformerLM
from model.train import save_checkpoint,load_checkpoint

class TransformerTests(unittest.TestCase):
    def tiny(self): return MediaTransformerLM(TransformerConfig(vocab_size=64,max_seq_len=32,d_model=32,n_heads=4,n_layers=2,d_ff=64,dropout=0.0))
    def test_forward_shape_and_loss(self):
        torch.manual_seed(1); m=self.tiny(); x=torch.randint(0,64,(3,12)); logits,loss=m(x,x); self.assertEqual(tuple(logits.shape),(3,12,64)); self.assertTrue(torch.isfinite(loss))
    def test_backward_gradients(self):
        torch.manual_seed(2); m=self.tiny(); x=torch.randint(0,64,(2,10)); _,loss=m(x,x); loss.backward(); self.assertTrue(any(p.grad is not None and torch.isfinite(p.grad).all() and p.grad.abs().sum()>0 for p in m.parameters()))
    def test_causal_mask(self):
        torch.manual_seed(3); m=self.tiny().eval(); x=torch.tensor([[1,2,3,4,5,6]]); y,_=m(x); z=x.clone(); z[0,-1]=17; y2,_=m(z); self.assertTrue(torch.allclose(y[:,:-1],y2[:,:-1],atol=1e-6,rtol=1e-6))
    def test_checkpoint_roundtrip(self):
        torch.manual_seed(4); m=self.tiny(); o=torch.optim.AdamW(m.parameters(),lr=1e-3); x=torch.randint(0,64,(2,8)); _,loss=m(x,x); loss.backward(); o.step()
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"model.pt"; save_checkpoint(p,m,o,7,float(loss)); r=self.tiny(); ro=torch.optim.AdamW(r.parameters(),lr=1e-3); step,saved=load_checkpoint(p,r,ro); self.assertEqual(step,7); self.assertAlmostEqual(saved,float(loss),places=6)
            for a,b in zip(m.parameters(),r.parameters()): self.assertTrue(torch.equal(a,b))
    def test_tiny_corpus_overfit(self):
        torch.manual_seed(5); m=MediaTransformerLM(TransformerConfig(vocab_size=16,max_seq_len=12,d_model=32,n_heads=4,n_layers=2,d_ff=64,dropout=0.0)); o=torch.optim.AdamW(m.parameters(),lr=5e-3); x=torch.tensor([[1,2,3,4,5,6,7,8]]); losses=[]
        for _ in range(60): o.zero_grad(set_to_none=True); _,loss=m(x,x); loss.backward(); o.step(); losses.append(float(loss))
        self.assertLess(losses[-1],losses[0]*.25)
    def test_generation(self):
        torch.manual_seed(6); m=self.tiny().eval(); out=m.generate(torch.tensor([[1,2,3]]),5); self.assertEqual(out.shape,(1,8))
if __name__=="__main__": unittest.main()
