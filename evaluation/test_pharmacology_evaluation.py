import unittest
from pathlib import Path

from run_evaluation import run

ROOT = Path(__file__).resolve().parent

CASES = [
    ("pharmacology_anticoagulants_fibrinolytics_eval.json", 6),
    ("pharmacology_nutritional_anemia_treatment_eval.json", 6),
    ("pharmacology_antimalarial_agents_eval.json", 6),
    ("pharmacology_anticancer_drugs_eval.json", 6),
    ("pharmacology_leishmaniasis_filariasis_treatment_eval.json", 6),
    ("pharmacology_toxoplasmosis_trypanosomiasis_treatment_eval.json", 6),
    ("pharmacology_medicine_malaria_filariasis_leishmaniasis_eval.json", 6),
    ("medicine_bone_marrow_aspiration_biopsy_eval.json", 6),
    ("medicine_transplantation_eval.json", 6),
    ("medicine_blood_transfusion_precautions_reactions_eval.json", 6),
    ("pediatrics_nutritional_anemias_eval.json", 6),
    ("pediatrics_aplastic_anemia_eval.json", 6),
    ("pediatrics_hemolytic_anemias_eval.json", 6),
    ("pediatrics_bleeding_disorders_eval.json", 6),
]

class PharmacologyEvaluationTests(unittest.TestCase):
    def test_all_hematology_evaluations_pass(self):
        for filename, expected_total in CASES:
            with self.subTest(filename=filename):
                report = run(ROOT / filename)
                self.assertEqual(report["total_cases"], expected_total)
                self.assertEqual(report["failed_cases"], 0)
                self.assertEqual(report["pass_rate"], 1.0)

if __name__ == "__main__":
    unittest.main()
