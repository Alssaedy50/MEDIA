import unittest
from pathlib import Path

from retrieval import DEFAULT_KB, load_records, retrieve


class HematologyRetrievalSmokeTests(unittest.TestCase):
    def test_knowledge_scope_is_loaded(self):
        records = load_records(DEFAULT_KB)
        self.assertEqual(len(records), 51)
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

    def test_antimalarial_query_retrieves_pharmacology(self):
        result = retrieve("What are the main principles of antimalarial pharmacology?", top_k=3)
        self.assertFalse(result["abstain"])
        ids = [hit["id"] for hit in result["hits"]]
        self.assertIn("hematology.pharmacology.antimalarial_agents", ids)

    def test_transfusion_query_retrieves_medicine(self):
        result = retrieve("What are the essential precautions for safe blood transfusion?", top_k=3)
        self.assertFalse(result["abstain"])
        ids = [hit["id"] for hit in result["hits"]]
        self.assertIn("hematology.medicine.blood_transfusion_precautions_reactions", ids)


    def test_blood_borne_infections_query_retrieves_community_medicine(self):
        result = retrieve("How can blood-borne infections be prevented in the community?", top_k=3)
        self.assertFalse(result["abstain"])
        ids = [hit["id"] for hit in result["hits"]]
        self.assertIn("hematology.community_medicine.blood_borne_infections", ids)

    def test_anemia_public_health_query_retrieves_community_medicine(self):
        result = retrieve("Why is anemia a public health problem?", top_k=3)
        self.assertFalse(result["abstain"])
        ids = [hit["id"] for hit in result["hits"]]
        self.assertIn("hematology.community_medicine.anemia_public_health", ids)

    def test_unknown_query_abstains(self):
        result = retrieve("What is the molecular mechanism of a fictional drug called Xylomab?", top_k=3)
        self.assertTrue(result["abstain"])
        self.assertEqual(result["hits"], [])

    def test_blood_groups_phrase_retrieves_blood_groups(self):
        result = retrieve(
            "Why can an ABO-incompatible red blood cell transfusion cause acute hemolysis?",
            top_k=3,
        )
        self.assertFalse(result["abstain"])
        self.assertIn("hematology.physiology.blood_groups", [h["id"] for h in result["hits"]])

    def test_lymphatic_filariasis_phrase_retrieves_filariasis(self):
        result = retrieve(
            "What is the causative organism and main transmission route of Lymphatic Filariasis?",
            top_k=3,
        )
        self.assertFalse(result["abstain"])
        self.assertIn("hematology.microbiology.lymphatic_filariasis", [h["id"] for h in result["hits"]])


if __name__ == "__main__":
    unittest.main()
