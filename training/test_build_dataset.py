import unittest
import build_dataset

class TrainingDatasetPipelineTests(unittest.TestCase):
    def test_split_is_stable(self):
        self.assertEqual(
            build_dataset.split_for("hematology.pathology.aplastic_anemia"),
            build_dataset.split_for("hematology.pathology.aplastic_anemia"),
        )

    def test_record_generates_traceable_examples(self):
        record = {
            "id":"hematology.test.example","topic":"Example","concept":"Example concept",
            "status":"reviewed","sources":[{"title":"Example source"}],
            "content":{"definition":"A controlled test definition.","mechanism":"A controlled test mechanism.",
                       "high_yield":["A controlled high-yield point."]},
            "terminology":[{"term":"example","arabic":"مثال","notes":"A controlled terminology note.",
                            "pronunciation":"ig-ZAM-pul"}]
        }
        rows = build_dataset.build_record_rows(record)
        self.assertGreaterEqual(len(rows), 5)
        self.assertTrue(all(r["record_id"] == record["id"] for r in rows))
        self.assertTrue(all(r["provenance"]["type"] == "knowledge_derived" for r in rows))

    def test_record_cannot_span_multiple_splits(self):
        record = {
            "id":"hematology.test.example2","topic":"Example","concept":"Example concept",
            "status":"reviewed","sources":[{"title":"Example source"}],
            "content":{"definition":"A controlled test definition."}
        }
        rows = build_dataset.build_record_rows(record)
        self.assertEqual({r["split"] for r in rows}, {build_dataset.split_for(record["id"])})

    def test_abstention_is_test_only(self):
        self.assertTrue(build_dataset.ABSTENTION)
        self.assertTrue(all(r["split"] == "test" for r in build_dataset.ABSTENTION))
        self.assertTrue(all(r["task"] == "abstention" for r in build_dataset.ABSTENTION))

if __name__ == "__main__":
    unittest.main()
