package com.media.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Pure-JVM tests for the offline matcher. These deliberately avoid {@code org.json} so they run
 * without Robolectric; the record view is built from plain strings.
 */
public class OfflineKnowledgeTest {

    private static OfflineKnowledge.DocumentText doc(String topic, String concept, String content) {
        return OfflineKnowledge.DocumentText.of(topic, "", concept, "Hematology", "Physiology", content, "");
    }

    private static double score(String query, OfflineKnowledge.DocumentText doc) {
        return OfflineKnowledge.score(OfflineKnowledge.QueryPlan.of(query), doc);
    }

    @Test
    public void abbreviationDictionaryCoversCoreHematologyAcronyms() {
        String[] expected = {"cbc", "epo", "ida", "rbc", "wbc", "plt", "mcv", "esr"};
        for (String abbreviation : expected) {
            assertTrue("missing abbreviation " + abbreviation,
                    OfflineKnowledge.ABBREVIATIONS.containsKey(abbreviation));
        }
        assertEquals("complete blood count", OfflineKnowledge.ABBREVIATIONS.get("cbc"));
        assertEquals("erythropoietin", OfflineKnowledge.ABBREVIATIONS.get("epo"));
        assertEquals("iron deficiency anemia", OfflineKnowledge.ABBREVIATIONS.get("ida"));
        assertEquals("red blood cell", OfflineKnowledge.ABBREVIATIONS.get("rbc"));
        assertEquals("white blood cell", OfflineKnowledge.ABBREVIATIONS.get("wbc"));
        assertEquals("platelet", OfflineKnowledge.ABBREVIATIONS.get("plt"));
        assertEquals("mean corpuscular volume", OfflineKnowledge.ABBREVIATIONS.get("mcv"));
        assertEquals("erythrocyte sedimentation rate", OfflineKnowledge.ABBREVIATIONS.get("esr"));
    }

    @Test
    public void bareAbbreviationExpandsToCanonicalPhrase() {
        OfflineKnowledge.QueryPlan plan = OfflineKnowledge.QueryPlan.of("CBC");
        assertEquals("complete blood count", plan.phrase);
        assertTrue(plan.pureAbbreviation);
        assertTrue(plan.tokens.contains("complete"));
        assertTrue(plan.tokens.contains("blood"));
        assertTrue(plan.tokens.contains("count"));
        assertFalse(plan.isEmpty());
    }

    @Test
    public void cbcPrioritizesRecordThatDefinesCompleteBloodCount() {
        OfflineKnowledge.DocumentText defining = doc("Complete Blood Count",
                "The complete blood count measures formed elements of blood.",
                "diagnosis complete blood count with differential");
        OfflineKnowledge.DocumentText passing = doc("Granular and Agranular Leukocytes",
                "Leukocyte structure and classification.",
                "A WBC differential is part of the CBC.");

        double definingScore = score("CBC", defining);
        double passingScore = score("CBC", passing);

        assertTrue(definingScore >= OfflineKnowledge.CONFIDENCE_THRESHOLD);
        assertTrue("CBC should rank the defining record first",
                definingScore > passingScore);
    }

    @Test
    public void cbcStillMatchesPlainCompleteBloodCountQuery() {
        OfflineKnowledge.DocumentText defining = doc("Blood Tissue",
                "Blood is a specialized connective tissue.",
                "tested with complete blood count with differential");
        assertTrue(score("complete blood count", defining) >= OfflineKnowledge.CONFIDENCE_THRESHOLD);
    }

    @Test
    public void epoResolvesToErythropoietin() {
        OfflineKnowledge.DocumentText erythro = doc(
                "Red Blood Cell Structure, Function and Erythropoiesis",
                "Erythropoiesis is regulated by erythropoietin.",
                "mechanism erythropoietin stimulates erythroid progenitors");
        OfflineKnowledge.DocumentText unrelated = doc("Toxoplasmosis",
                "A protozoal infection.", "cats are definitive hosts");
        assertTrue(score("EPO", erythro) >= OfflineKnowledge.CONFIDENCE_THRESHOLD);
        assertTrue(score("EPO", erythro) > score("EPO", unrelated));
    }

    @Test
    public void idaResolvesToIronDeficiencyAnemia() {
        OfflineKnowledge.DocumentText anemia = doc(
                "Anemia Classification and Nutritional Anemias",
                "Nutritional anemias include iron deficiency anemia.",
                "iron deficiency anemia is microcytic and hypochromic");
        assertTrue(score("IDA", anemia) >= OfflineKnowledge.CONFIDENCE_THRESHOLD);
    }

    @Test
    public void rbcWbcPltAndMcvResolveToTheirConcepts() {
        String[] queries = {"RBC", "WBC", "PLT", "MCV"};
        String[] phrases = {"red blood cell", "white blood cell", "platelet", "mean corpuscular volume"};
        for (int i = 0; i < queries.length; i++) {
            OfflineKnowledge.QueryPlan plan = OfflineKnowledge.QueryPlan.of(queries[i]);
            assertEquals(phrases[i], plan.phrase);
            assertTrue(plan.pureAbbreviation);
        }
    }

    @Test
    public void esrMapsToErythrocyteSedimentationRateEvenWithoutACorpusMatch() {
        OfflineKnowledge.QueryPlan plan = OfflineKnowledge.QueryPlan.of("ESR");
        assertEquals("erythrocyte sedimentation rate", plan.phrase);
        assertTrue(plan.tokens.contains("sedimentation"));
        assertTrue(plan.tokens.contains("rate"));
    }

    @Test
    public void mixedAbbreviationQueryExpandsWithoutBecomingPure() {
        OfflineKnowledge.QueryPlan plan = OfflineKnowledge.QueryPlan.of("CBC with differential");
        assertFalse(plan.pureAbbreviation);
        assertEquals(1, plan.expansions.size());
        assertTrue(plan.tokens.contains("differential"));
        assertTrue(plan.tokens.contains("complete"));
    }

    @Test
    public void unrelatedQueryScoresBelowConfidenceThreshold() {
        OfflineKnowledge.DocumentText blood = doc("Blood Tissue",
                "Blood is a specialized connective tissue.",
                "plasma and formed elements");
        double nonsense = score("quantum chromodynamics lecture", blood);
        assertTrue("irrelevant query must stay below abstention threshold",
                nonsense < OfflineKnowledge.CONFIDENCE_THRESHOLD);
    }

    @Test
    public void emptyAndStopWordOnlyQueriesAreEmpty() {
        assertTrue(OfflineKnowledge.QueryPlan.of("   ").isEmpty());
        assertTrue(OfflineKnowledge.QueryPlan.of("what is the").isEmpty());
    }

    @Test
    public void punctuationAndCaseAreNormalized() {
        assertEquals("what is bone marrow", OfflineKnowledge.normalize("What is Bone Marrow?"));
        assertEquals("cbc", OfflineKnowledge.normalize("CBC,"));
        List<String> tokens = OfflineKnowledge.tokens("What is the bone marrow?");
        assertTrue(tokens.contains("bone"));
        assertTrue(tokens.contains("marrow"));
        assertFalse(tokens.contains("what"));
        assertFalse(tokens.contains("the"));
    }

    @Test
    public void occurrencesUseWholeWordBoundaries() {
        assertEquals(2, OfflineKnowledge.occurrences("cbc and cbc again", "cbc"));
        assertEquals(0, OfflineKnowledge.occurrences("brazil", "id"));
        assertEquals(0, OfflineKnowledge.occurrences("", "cbc"));
        assertEquals(0, OfflineKnowledge.occurrences("cbc", ""));
        assertEquals(1, OfflineKnowledge.occurrences("iron deficiency anemia present", "iron deficiency anemia"));
    }

    @Test
    public void arabicTextIsPreservedDuringNormalization() {
        assertEquals("فقر الدم", OfflineKnowledge.normalize("فقر الدم؟"));
    }
}
