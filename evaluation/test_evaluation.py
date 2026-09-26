#!/usr/bin/env python3

import unittest
from pathlib import Path

from run_evaluation import run


ROOT = Path(__file__).resolve().parent


class EvaluationTests(unittest.TestCase):
    def test_histology_evaluation_passes(self):
        report = run(ROOT / "histology_eval.json")
        self.assertEqual(report["total_cases"], 5)
        self.assertEqual(report["failed_cases"], 0)
        self.assertEqual(report["pass_rate"], 1.0)

    def test_leukocyte_evaluation_passes(self):
        report = run(ROOT / "histology_leukocytes_eval.json")
        self.assertEqual(report["total_cases"], 6)
        self.assertEqual(report["failed_cases"], 0)
        self.assertEqual(report["pass_rate"], 1.0)

    def test_anatomy_evaluation_passes(self):
        report = run()
        self.assertEqual(report["total_cases"], 8)
        self.assertEqual(report["failed_cases"], 0)
        self.assertEqual(report["pass_rate"], 1.0)


if __name__ == "__main__":
    unittest.main()
