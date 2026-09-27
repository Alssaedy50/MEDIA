package com.media.android.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Query-understanding tests for the bilingual lexicon and normaliser. */
public class MedicalLexiconTest {

    @Test
    public void coreHematologyAcronymsAreMapped() {
        assertEquals("complete blood count", MedicalLexicon.canonicalFor("cbc"));
        assertEquals("erythropoietin", MedicalLexicon.canonicalFor("epo"));
        assertEquals("iron deficiency anemia", MedicalLexicon.canonicalFor("ida"));
        assertEquals("red blood cell", MedicalLexicon.canonicalFor("rbc"));
        assertEquals("white blood cell", MedicalLexicon.canonicalFor("wbc"));
        assertEquals("platelet", MedicalLexicon.canonicalFor("plt"));
        assertEquals("mean corpuscular volume", MedicalLexicon.canonicalFor("mcv"));
        assertEquals("erythrocyte sedimentation rate", MedicalLexicon.canonicalFor("esr"));
    }

    @Test
    public void cbcHasMultipleEnglishSurfaceForms() {
        assertEquals("complete blood count", MedicalLexicon.canonicalFor("full blood count"));
        assertEquals("complete blood count", MedicalLexicon.canonicalFor("blood count"));
        assertEquals("complete blood count", MedicalLexicon.canonicalFor("fbc"));
    }

    @Test
    public void cbcHasArabicSurfaceForms() {
        assertEquals("complete blood count", MedicalLexicon.canonicalFor(Text.normalize("تحليل الدم")));
        assertEquals("complete blood count", MedicalLexicon.canonicalFor(Text.normalize("صورة الدم الكاملة")));
        assertEquals("complete blood count", MedicalLexicon.canonicalFor(Text.normalize("تعداد الدم الكامل")));
    }

    @Test
    public void abbreviationQueryIsDetectedAndExpanded() {
        KnowledgeRepository.QueryPlan plan = KnowledgeRepository.QueryPlan.of("CBC");
        assertEquals("complete blood count", plan.phrase);
        assertTrue(plan.pureAbbreviation);
        assertTrue(plan.tokens.contains("complete"));
        assertTrue(plan.tokens.contains("blood"));
        assertTrue(plan.tokens.contains("count"));
        assertFalse(plan.isEmpty());
    }

    @Test
    public void mixedAbbreviationQueryIsNotPureButStillExpands() {
        KnowledgeRepository.QueryPlan plan = KnowledgeRepository.QueryPlan.of("CBC with differential");
        assertFalse(plan.pureAbbreviation);
        assertTrue(plan.tokens.contains("differential"));
        assertTrue(plan.tokens.contains("complete"));
    }

    @Test
    public void multiWordAliasExpandsInsideALongerQuery() {
        String expanded = MedicalLexicon.expand(Text.normalize("explain full blood count results"));
        assertTrue(expanded.contains("complete blood count"));
        assertTrue(expanded.contains("explain"));
    }

    @Test
    public void unknownQueryHasNoAlias() {
        assertNull(MedicalLexicon.canonicalFor("quantum chromodynamics"));
        assertFalse(MedicalLexicon.isAliasQuery("quantum chromodynamics"));
    }

    @Test
    public void normalizationFoldsCaseAndPunctuation() {
        assertEquals("what is bone marrow", Text.normalize("What is Bone Marrow?"));
        assertEquals("cbc", Text.normalize("CBC,"));
    }

    @Test
    public void arabicFoldingUnifiesOrthographicVariants() {
        // alef with hamza and plain alef must fold to the same normalised form
        assertEquals(Text.normalize("إرثروبويتين"), Text.normalize("ارثروبويتين"));
        // teh marbuta folds to heh
        assertEquals(Text.normalize("كريه"), Text.normalize("كرية"));
    }

    @Test
    public void arabicTextIsPreservedDuringNormalization() {
        assertEquals("فقر الدم", Text.normalize("فقر الدم؟"));
    }

    @Test
    public void tokensDropStopWordsAndShortFragments() {
        List<String> tokens = Text.tokens("What is the bone marrow?");
        assertTrue(tokens.contains("bone"));
        assertTrue(tokens.contains("marrow"));
        assertFalse(tokens.contains("what"));
        assertFalse(tokens.contains("the"));
    }

    @Test
    public void arabicStopWordsAreDropped() {
        List<String> tokens = Text.tokens("ما هو نخاع العظم");
        assertFalse(tokens.contains("ما"));
        assertFalse(tokens.contains("هو"));
        assertTrue(tokens.contains("نخاع"));
    }

    @Test
    public void emptyAndStopWordOnlyQueriesAreEmpty() {
        assertTrue(KnowledgeRepository.QueryPlan.of("   ").isEmpty());
        assertTrue(KnowledgeRepository.QueryPlan.of("what is the").isEmpty());
    }

    @Test
    public void occurrencesUseWholeWordBoundaries() {
        assertEquals(2, Text.occurrences("cbc and cbc again", "cbc"));
        assertEquals(0, Text.occurrences("brazil", "id"));
        assertEquals(0, Text.occurrences("", "cbc"));
        assertEquals(0, Text.occurrences("cbc", ""));
        assertEquals(1, Text.occurrences("iron deficiency anemia present", "iron deficiency anemia"));
    }

    @Test
    public void aliasLookupIsAvailableForDiagnostics() {
        assertNotNull(MedicalLexicon.aliases());
        assertTrue(MedicalLexicon.aliases().size() > 50);
    }
}
