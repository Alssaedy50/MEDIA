import unittest
from model.size import find_near_target, parameter_count, choose_heads

class ModelSizingTests(unittest.TestCase):
    def test_parameter_formula_with_tied_embeddings(self):
        self.assertEqual(parameter_count(100, 16, 8, 2, 16), 2032)

    def test_untied_adds_output_matrix(self):
        tied = parameter_count(100, 16, 8, 2, 16)
        untied = parameter_count(100, 16, 8, 2, 16, tied_embeddings=False)
        self.assertEqual(untied - tied, 800)

    def test_head_selection(self):
        self.assertEqual(choose_heads(832), 8)

    def test_search_returns_exact_architectures(self):
        result = find_near_target(50_000, max_results=5)
        self.assertEqual(len(result), 5)
        self.assertTrue(all(x.parameter_count > 0 for x in result))
        self.assertEqual(result, sorted(result, key=lambda x: abs(x.parameter_count - 100_000_000)))

if __name__ == "__main__":
    unittest.main()
