#!/usr/bin/env python3

import unittest

from run_evaluation import run


class EvaluationTests(unittest.TestCase):
    def test_anatomy_evaluation_passes(self):
        report = run()
        self.assertEqual(report["total_cases"], 14)
        self.assertEqual(report["failed_cases"], 0)
        self.assertEqual(report["pass_rate"], 1.0)
        self.assertEqual(report["supported_passed"], report["supported_cases"])
        self.assertEqual(report["abstention_passed"], report["abstention_cases"])
        self.assertEqual(report["source_traceability_passed"], report["total_cases"])


if __name__ == "__main__":
    unittest.main()
