import unittest

from app.api.main import AnswerRequest, api_answer, health, runtime, scope


class TestMediaApi(unittest.TestCase):
    def test_health(self):
        self.assertEqual(health()["status"], "ok")
        self.assertEqual(health()["version"], "0.2.0")

    def test_scope(self):
        data = scope()
        self.assertEqual(data["knowledge_scope"], "knowledge/hematology")
        self.assertTrue(data["evidence_grounded"])
        self.assertTrue(data["safe_abstention"])

    def test_runtime_status_is_explicit_without_weights(self):
        data = runtime()
        self.assertEqual(data["foundation_status"], "frozen")
        self.assertEqual(data["weights_available"], False)
        self.assertIn("architecture", data)

    def test_supported_question(self):
        data = api_answer(AnswerRequest(
            query="What is the function of red blood cells?",
            mode="quick",
            top_k=3,
        ))
        self.assertFalse(data["abstain"])
        self.assertTrue(data["retrieved_ids"])
        self.assertEqual(data["knowledge_scope"], "knowledge/hematology")

    def test_unknown_named_term_abstains(self):
        data = api_answer(AnswerRequest(
            query="Explain HemoLysis-X99",
            mode="quick",
            top_k=3,
        ))
        self.assertTrue(data["abstain"])
        self.assertEqual(data["evidence_state"], "insufficient")
        self.assertEqual(data["retrieved_ids"], [])


if __name__ == "__main__":
    unittest.main()
