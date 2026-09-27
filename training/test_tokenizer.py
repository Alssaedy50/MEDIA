import tempfile
import unittest
from pathlib import Path
from training.tokenizer import BPETokenizer, normalize, pretokenize, train_vocab

class TokenizerTests(unittest.TestCase):
    def test_normalization_and_bilingual_pretokenization(self):
        text="  HbA1c   والهيموغلوبين  "
        self.assertEqual(normalize(text),"HbA1c والهيموغلوبين")
        tokens=pretokenize(text)
        self.assertIn("HbA1c",tokens); self.assertIn("والهيموغلوبين",tokens)
    def test_training_is_deterministic(self):
        texts=["red blood cell","red blood cell","خلايا الدم الحمراء","خلايا الدم الحمراء"]
        a=train_vocab(texts,vocab_size=128); b=train_vocab(texts,vocab_size=128)
        self.assertEqual(a,b); self.assertLessEqual(a["vocab_size"],128)
    def test_round_trip_preserves_medical_words(self):
        config=train_vocab(["hemoglobin erythropoiesis","هيموغلوبين كريات الدم الحمراء"],vocab_size=256,min_frequency=1)
        tokenizer=BPETokenizer(config); encoded=tokenizer.encode("hemoglobin هيموغلوبين")
        self.assertEqual(encoded[0],tokenizer.bos_id); self.assertEqual(encoded[-1],tokenizer.eos_id)
        decoded=tokenizer.decode(encoded)
        self.assertIn("hemoglobin",decoded); self.assertIn("هيموغلوبين",decoded)
    def test_save_and_load(self):
        config=train_vocab(["blood cells","خلايا الدم"],vocab_size=128,min_frequency=1)
        tokenizer=BPETokenizer(config)
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/"tokenizer.json"; tokenizer.save(path); loaded=BPETokenizer.load(path)
            self.assertEqual(tokenizer.encode("blood خلايا"),loaded.encode("blood خلايا"))
if __name__=="__main__": unittest.main()
