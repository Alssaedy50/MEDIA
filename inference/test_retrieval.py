import unittest
from pathlib import Path

from retrieval import DEFAULT_KB, load_records, retrieve


class AnatomyRetrievalSmokeTests(unittest.TestCase):
    def test_knowledge_scope_is_loaded(self):
        records = load_records(DEFAULT_KB)
        self.assertEqual(len(records), 6)
        self.assertTrue(all(r.data.get("status") in {"reviewed", "verified"} for r in records))

    def test_red_pulp_query_retrieves_spleen(self):
        result = retrieve("What is the function of the red pulp?", top_k=3)
        self.assertFalse(result["abstain"])
        ids = [hit["id"] for hit in result["hits"]]
        self.assertIn("hematology.anatomy.spleen", ids)

    def test_prox1_query_retrieves_embryology(self):
        result = retrieve("What is the role of PROX1 in lymphatic development?", top_k=3)
        self.assertFalse(result["abstain"])
        ids = [hit["id"] for hit in result["hits"]]
        self.assertIn("hematology.anatomy.embryology_hemopoietic_lymphatic", ids)

    def test_bone_marrow_query_retrieves_bone_marrow(self):
        result = retrieve("Where does hematopoiesis occur in bone marrow?", top_k=3)
        self.assertFalse(result["abstain"])
        self.assertEqual(result["hits"][0]["id"], "hematology.anatomy.bone_marrow")

    def test_unknown_query_abstains(self):
        result = retrieve("What is the molecular mechanism of a fictional drug called Xylomab?", top_k=3)
        self.assertTrue(result["abstain"])
        self.assertEqual(result["hits"], [])


if __name__ == "__main__":
    unittest.main()
