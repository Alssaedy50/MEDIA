#!/usr/bin/env python3

import unittest
from pathlib import Path

from run_evaluation import run


ROOT = Path(__file__).resolve().parent


class StemCellPathologyEvaluationTests(unittest.TestCase):
    def test_stem_cell_pathology_evaluation_passes(self):
        report = run(ROOT / "pathology_stem_cell_eval.json")
        self.assertEqual(report["total_cases"], 6)
        self.assertEqual(report["failed_cases"], 0)
        self.assertEqual(report["pass_rate"], 1.0)


if __name__ == "__main__":
    unittest.main()
