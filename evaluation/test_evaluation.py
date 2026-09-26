#!/usr/bin/env python3

import unittest

from run_evaluation import run


class EvaluationTests(unittest.TestCase):
    def test_anatomy_evaluation_passes(self):
        report = run()
        self.assertEqual(report["total_cases"], 8)
        self.assertEqual(report["failed_cases"], 0)
        self.assertEqual(report["pass_rate"], 1.0)


if __name__ == "__main__":
    unittest.main()
