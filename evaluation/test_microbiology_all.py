import unittest
from pathlib import Path
from run_evaluation import run

ROOT = Path(__file__).resolve().parent

class MicrobiologyEvaluationSuite(unittest.TestCase):
    def test_all_microbiology_evaluations_pass(self):
        files = sorted(ROOT.glob("microbiology_*_eval.json"))
        self.assertGreaterEqual(len(files), 11)
        for path in files:
            with self.subTest(path=path.name):
                report = run(path)
                self.assertEqual(report["total_cases"], 6)
                self.assertEqual(report["failed_cases"], 0)
                self.assertEqual(report["pass_rate"], 1.0)

if __name__ == "__main__":
    unittest.main()
