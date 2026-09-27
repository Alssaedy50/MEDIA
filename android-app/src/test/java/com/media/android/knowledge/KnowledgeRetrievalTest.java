package com.media.android.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.media.android.ai.GenerationOptions;
import com.media.android.ai.GenerationResult;
import com.media.android.ai.engine.LocalKnowledgeEngine;
import com.media.android.ai.engine.MediaEngineRegistry;
import com.media.android.ai.orchestrator.IntentDetector;
import com.media.android.ai.orchestrator.MediaAiOrchestrator;
import com.media.android.ai.orchestrator.OrchestratedResponse;
import com.media.android.data.HistoryEntry;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * End-to-end tests over the real offline corpus.
 *
 * <p>These exercise the actual retrieval, composition and orchestration code paths against the
 * JSON records shipped in the APK, using a file-backed {@link KnowledgeSource}. No mocks: the
 * files read here are the same ones packaged into {@code assets/}.</p>
 */
public class KnowledgeRetrievalTest {

    private static KnowledgeRepository repository;

    @BeforeClass
    public static void loadCorpus() {
        File root = locateAssets();
        repository = new KnowledgeRepository(new FileKnowledgeSource(root));
        repository.ensureEnumerated();
    }

    /** Walks up from the Gradle working directory to find the repository-level assets tree. */
    private static File locateAssets() {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 6 && dir != null; i++) {
            File candidate = new File(dir, "android/assets");
            if (new File(candidate, "knowledge/hematology").isDirectory()) return candidate;
            File direct = new File(dir, "assets");
            if (new File(direct, "knowledge/hematology").isDirectory()) return direct;
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("Could not locate android/assets knowledge tree");
    }

    @Test
    public void corpusIsEnumeratedWithoutLoadFailures() {
        assertTrue("expected a non-trivial corpus", repository.size() >= 40);
        assertEquals(0, repository.loadFailures());
        assertTrue(repository.isReady());
    }

    @Test
    public void cbcAbbreviationResolvesToCompleteBloodCountEvidence() {
        KnowledgeRepository.Result result = repository.best("CBC");
        assertNotNull(result);
        assertTrue("CBC must reach a confident local match", repository.isConfident(result));
        String answer = AnswerComposer.compose(result);
        assertTrue(answer.contains("complete blood count"));
    }

    @Test
    public void arabicAliasForCbcResolves() {
        KnowledgeRepository.Result result = repository.best("تعداد الدم الكامل");
        assertNotNull("Arabic CBC alias must retrieve evidence", result);
        assertTrue(repository.isConfident(result));
    }

    @Test
    public void epoAbbreviationResolvesToErythropoietin() {
        KnowledgeRepository.Result result = repository.best("EPO");
        assertNotNull(result);
        assertTrue(repository.isConfident(result));
        assertTrue(AnswerComposer.compose(result).toLowerCase().contains("erythropoietin"));
    }

    @Test
    public void erythropoiesisQuestionRetrievesTheRightRecord() {
        KnowledgeRepository.Result result = repository.best("Explain erythropoiesis and its regulation");
        assertNotNull(result);
        assertTrue(result.score >= KnowledgeRepository.CONFIDENCE_THRESHOLD);
        assertTrue(result.record.title().toLowerCase().contains("erythropoiesis")
                || result.record.concept().toLowerCase().contains("erythropoiesis"));
    }

    @Test
    public void unrelatedQueryAbstainsBelowConfidenceThreshold() {
        KnowledgeRepository.Result result = repository.best("quantum chromodynamics lecture");
        assertTrue(result == null || !repository.isConfident(result));
    }

    @Test
    public void composedAnswerIsStructuredAndBilingual() {
        KnowledgeRepository.Result result = repository.best("aplastic anemia");
        assertNotNull(result);
        String answer = AnswerComposer.compose(result);
        assertTrue("answer should carry a heading", answer.startsWith("# "));
        assertTrue("answer should carry Arabic section labels", answer.contains("التعريف")
                || answer.contains("الشرح"));
        assertTrue("answer should carry an evidence section", answer.contains("Evidence"));
        assertTrue("answer should state offline provenance", answer.contains("Offline retrieval"));
    }

    @Test
    public void intentDetectionClassifiesRepresentativeQueries() {
        assertEquals(IntentDetector.Intent.TERM, IntentDetector.detect("CBC"));
        assertEquals(IntentDetector.Intent.EXPLAIN, IntentDetector.detect("What is aplastic anemia?"));
        assertEquals(IntentDetector.Intent.CLINICAL_CASE,
                IntentDetector.detect("Clinical case: a patient with microcytic anemia"));
        assertEquals(IntentDetector.Intent.STUDY,
                IntentDetector.detect("Study high yield points for leukemia"));
        assertEquals(IntentDetector.Intent.SUMMARY,
                IntentDetector.detect("Summarize hemostasis and coagulation"));
    }

    @Test
    public void orchestratorReturnsAnsweredForGroundedQuery() {
        MediaEngineRegistry registry = new MediaEngineRegistry();
        registry.register(new LocalKnowledgeEngine(repository));
        MediaAiOrchestrator orchestrator = new MediaAiOrchestrator(registry);
        OrchestratedResponse response = orchestrator.answer("Explain erythropoiesis");
        assertEquals(OrchestratedResponse.State.ANSWERED, response.state);
        assertFalse(response.content.isEmpty());
        assertFalse(response.evidenceIds.isEmpty());
        assertEquals(LocalKnowledgeEngine.ID, response.engineId);
    }

    @Test
    public void orchestratorAbstainsInsteadOfInventingContent() {
        MediaEngineRegistry registry = new MediaEngineRegistry();
        registry.register(new LocalKnowledgeEngine(repository));
        MediaAiOrchestrator orchestrator = new MediaAiOrchestrator(registry);
        OrchestratedResponse response = orchestrator.answer("write me a song about nanotubes");
        assertEquals(OrchestratedResponse.State.ABSTAINED, response.state);
        assertTrue(response.content.contains("NO SUPPORTED LOCAL MATCH"));
    }

    @Test
    public void orchestratorReportsNoEngineWhenKnowledgeIsEmpty() {
        MediaEngineRegistry registry = new MediaEngineRegistry();
        File empty = new File(System.getProperty("java.io.tmpdir"),
                "media-empty-" + System.nanoTime());
        assertTrue(empty.mkdirs() || empty.isDirectory());
        KnowledgeRepository emptyRepo = new KnowledgeRepository(new FileKnowledgeSource(empty));
        registry.register(new LocalKnowledgeEngine(emptyRepo));
        MediaAiOrchestrator orchestrator = new MediaAiOrchestrator(registry);
        OrchestratedResponse response = orchestrator.answer("CBC");
        assertEquals(OrchestratedResponse.State.NO_ENGINE, response.state);
    }

    @Test
    public void orchestratorRejectsBlankQuery() {
        MediaEngineRegistry registry = new MediaEngineRegistry();
        registry.register(new LocalKnowledgeEngine(repository));
        MediaAiOrchestrator orchestrator = new MediaAiOrchestrator(registry);
        assertEquals(OrchestratedResponse.State.ERROR, orchestrator.answer("   ").state);
    }

    @Test
    public void engineAbstainsWithGuidanceWhenLocalEvidenceIsWeak() {
        LocalKnowledgeEngine engine = new LocalKnowledgeEngine(repository);
        GenerationResult result = engine.generate("obscure unrelated phrase xyzzy",
                "", GenerationOptions.defaults());
        assertEquals(GenerationResult.Status.ABSTAINED, result.status);
        assertTrue(result.content.contains("MEDIA"));
    }

    @Test
    public void historyEntryRoundTripsCoreFields() {
        HistoryEntry entry = new HistoryEntry(null, "CBC", "answer", 0.81,
                "ANSWERED", LocalKnowledgeEngine.ID, 123L);
        assertTrue(entry.isAnswered());
        assertEquals("CBC", entry.question);
        assertEquals(0.81, entry.confidence, 0.0001);
    }

    /** Reads the packaged knowledge tree straight from the repository checkout. */
    private static final class FileKnowledgeSource implements KnowledgeSource {
        private final File root;

        FileKnowledgeSource(File root) {
            // Support both repository-root and assets-root working directories.
            this.root = new File(root, "knowledge/hematology").isDirectory()
                    ? root : new File(root, "android/assets");
        }

        @Override
        public List<String> list(String path) throws IOException {
            File dir = new File(root, path);
            if (!dir.isDirectory()) return new ArrayList<String>();
            String[] names = dir.list();
            return names == null ? new ArrayList<String>() : Arrays.asList(names);
        }

        @Override
        public String read(String path) throws IOException {
            File file = new File(root, path);
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
