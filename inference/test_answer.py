#!/usr/bin/env python3

import unittest

from answer import answer


class AnswerTests(unittest.TestCase):
    def test_quick_red_pulp_is_supported(self):
        result = answer("What is the function of the red pulp?", mode="quick")
        self.assertFalse(result["abstain"])
        self.assertEqual(result["evidence_state"], "supported")
        self.assertTrue(result["answer"])
        self.assertTrue(result["sources"])

    def test_explain_prox1_is_supported(self):
        result = answer("Explain the role of PROX1 in lymphatic development.", mode="explain")
        self.assertFalse(result["abstain"])
        self.assertIn("PROX1", " ".join(result["terms"]) + result["answer"])

    def test_compare_returns_multiple_concepts(self):
        result = answer("Compare spleen and lymph nodes.", mode="compare", top_k=3)
        self.assertFalse(result["abstain"])
        self.assertEqual(result["evidence_state"], "multi_concept")
        self.assertGreaterEqual(len(result["comparison"]), 2)
        concepts = {item["concept"] for item in result["comparison"]}
        self.assertTrue(any("Spleen" in concept for concept in concepts))
        self.assertTrue(any("Lymph Nodes" in concept for concept in concepts))

    def test_unknown_query_abstains(self):
        result = answer("What is the dose of XenoMed-9000 for anemia?", mode="quick")
        self.assertTrue(result["abstain"])
        self.assertEqual(result["evidence_state"], "insufficient")
        self.assertEqual(result["sources"], [])

    def test_exam_mode_keeps_high_yield_points(self):
        result = answer("What is the spleen?", mode="exam")
        self.assertFalse(result["abstain"])
        self.assertTrue(result["key_points"])


if __name__ == "__main__":
    unittest.main()
