package com.media.android.ai.engine;

import com.media.android.ai.GenerationOptions;
import com.media.android.ai.GenerationResult;
import com.media.android.knowledge.AnswerComposer;
import com.media.android.knowledge.KnowledgeRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, fully offline engine backed by the bundled reviewed knowledge records.
 *
 * <p>This is the engine that ships today. It performs evidence retrieval and abstains when the
 * local evidence is not strong enough; it never invents medical content. It is deliberately an
 * implementation of {@link MediaAiEngine} so it can later be composed with a neural engine inside a
 * hybrid orchestrator.</p>
 */
public final class LocalKnowledgeEngine implements MediaAiEngine {

    public static final String ID = "local-kb";

    private final KnowledgeRepository repository;

    public LocalKnowledgeEngine(KnowledgeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String id() { return ID; }

    @Override
    public boolean isAvailable() { return repository.size() > 0; }

    @Override
    public GenerationResult generate(String prompt, String context, GenerationOptions options) {
        KnowledgeRepository.Result best;
        try {
            best = repository.best(prompt);
        } catch (RuntimeException e) {
            return GenerationResult.error(ID, "Local retrieval failed: " + e.getMessage());
        }
        if (!repository.isConfident(best)) {
            return GenerationResult.abstained(abstention(prompt), ID);
        }
        List<String> evidence = new ArrayList<String>();
        evidence.add(best.record.id());
        boolean bilingual = options == null || options.preferArabicExplanation;
        String body = AnswerComposer.compose(best.record, best.score, bilingual);
        return GenerationResult.answered(body, best.score, evidence, ID);
    }

    /** Confidence of the best local match, or 0 when nothing is retrievable. */
    public double confidenceFor(String prompt) {
        KnowledgeRepository.Result best = repository.best(prompt);
        return best == null ? 0.0 : best.score;
    }

    private static String abstention(String query) {
        return "NO SUPPORTED LOCAL MATCH · لا توجد أدلة محلية كافية\n"
                + "---\n"
                + "MEDIA searched the offline Hematology library and did not find local evidence "
                + "strong enough to answer this with confidence, so it abstains rather than guessing.\n\n"
                + "لم يعثر MEDIA في مكتبة أمراض الدم المضمّنة على دليل محلي كافٍ للإجابة عن هذا السؤال "
                + "بثقة، ولذلك يلتزم الصمت بدلاً من التخمين.\n"
                + "---\n"
                + "## Your question · سؤالك\n"
                + query + "\n\n"
                + "## Try a specific medical term or acronym · جرّب مصطلحاً أو اختصاراً محدّداً\n"
                + "- Complete blood count — CBC · تحليل الدم الشامل\n"
                + "- Erythropoiesis · تكون الكريات الحمراء\n"
                + "- Iron deficiency anemia — IDA · فقر الدم بعوز الحديد\n"
                + "- Bone marrow · نخاع العظم\n"
                + "- Aplastic anemia · فقر الدم اللاتنسجي\n"
                + "- Sickle cell disease · مرض الخلايا المنجلية\n"
                + "- Erythropoietin — EPO · الإرثروبويتين\n\n"
                + "MEDIA runs fully offline and is currently scoped to the reviewed Hematology block. "
                + "The MEDIA foundation model is not installed yet, so answers come only from local "
                + "evidence.\n";
    }

    @Override
    public void close() {
        // Nothing to release: the knowledge layer is a plain in-memory index over assets.
    }
}
