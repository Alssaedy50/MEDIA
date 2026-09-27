package com.media.android.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bilingual medical query lexicon.
 *
 * <p>Maps student-typed surface forms (acronyms, English synonyms and Arabic equivalents) onto a
 * canonical English phrase that is the form used in the knowledge records. This is a
 * <em>query-understanding</em> aid only: it never adds medical facts, it only widens the way a
 * query can be spelled so that the local evidence layer is reachable through the terms a student
 * actually types. If a canonical phrase is absent from the bundled corpus, the query still ends in
 * an honest abstention.</p>
 */
public final class MedicalLexicon {

    /** Canonical phrases reused across several surface forms. */
    public static final String CBC = "complete blood count";
    public static final String RBC = "red blood cell";
    public static final String WBC = "white blood cell";
    public static final String PLATELET = "platelet";
    public static final String HEMOGLOBIN = "hemoglobin";

    /** Surface form (normalised) -> canonical phrase (normalised). */
    private static final Map<String, String> ALIASES = new LinkedHashMap<String, String>();

    private static void put(String surface, String canonical) {
        ALIASES.put(Text.normalize(surface), Text.normalize(canonical));
    }

    static {
        // ---- Haematology acronyms (offline dictionary; never leaves the device) ----
        put("cbc", CBC);
        put("fbc", CBC);
        put("fbcc", CBC);
        put("hb", HEMOGLOBIN);
        put("hgb", HEMOGLOBIN);
        put("hct", "hematocrit");
        put("pcv", "hematocrit");
        put("epo", "erythropoietin");
        put("ida", "iron deficiency anemia");
        put("rbc", RBC);
        put("wbc", WBC);
        put("plt", PLATELET);
        put("mcv", "mean corpuscular volume");
        put("mch", "mean corpuscular hemoglobin");
        put("mchc", "mean corpuscular hemoglobin concentration");
        put("rdw", "red cell distribution width");
        put("esr", "erythrocyte sedimentation rate");
        put("crp", "c reactive protein");
        put("pt", "prothrombin time");
        put("inr", "international normalized ratio");
        put("aptt", "activated partial thromboplastin time");
        put("vwd", "von willebrand disease");
        put("dic", "disseminated intravascular coagulation");
        put("cml", "chronic myeloid leukemia");
        put("aml", "acute myeloid leukemia");
        put("cll", "chronic lymphocytic leukemia");
        put("all", "acute lymphoblastic leukemia");
        put("mds", "myelodysplastic syndrome");
        put("g6pd", "glucose 6 phosphate dehydrogenase");
        put("hsct", "hematopoietic stem cell transplantation");
        put("b12", "vitamin b12");
        put("tibc", "total iron binding capacity");
        put("tsat", "transferrin saturation");
        put("bm", "bone marrow");
        put("ln", "lymph node");
        put("thpo", "thrombopoietin");

        // ---- English synonyms / spelling variants ----
        put("full blood count", CBC);
        put("blood count", CBC);
        put("blood picture", CBC);
        put("complete blood picture", CBC);
        put("cbc with differential", "complete blood count with differential");
        put("haemoglobin", HEMOGLOBIN);
        put("haematocrit", "hematocrit");
        put("erythrocyte", RBC);
        put("red blood cells", RBC);
        put("leucocyte", WBC);
        put("leukocyte", WBC);
        put("white blood cells", WBC);
        put("thrombocyte", PLATELET);
        put("platelets", PLATELET);
        put("anaemia", "anemia");
        put("aplastic anaemia", "aplastic anemia");
        put("iron deficiency anaemia", "iron deficiency anemia");
        put("sickle cell anaemia", "sickle cell anemia");
        put("sickle cell disease", "sickle cell disease");
        put("hemolytic anaemia", "hemolytic anemia");
        put("haemolytic anaemia", "hemolytic anemia");
        put("haemophilia", "hemophilia");
        put("haemostasis", "hemostasis");
        put("homeostasis coagulation", "hemostasis coagulation");
        put("leukaemia", "leukemia");
        put("leukaemias", "leukemia");
        put("lymphoma", "lymphoid tissue disorders");
        put("erythropoiesis", "erythropoiesis");
        put("haematopoiesis", "hematopoiesis");
        put("hematopoiesis", "hematopoiesis");
        put("blood group", "blood groups");
        put("blood grouping", "blood groups");
        put("abo blood group", "blood groups");
        put("rh blood group", "blood groups");
        put("coagulation cascade", "hemostasis coagulation");
        put("clotting cascade", "hemostasis coagulation");
        put("clotting disorder", "bleeding disorders");
        put("bleeding disorder", "bleeding disorders");
        put("polycythemia", "polycythemia");
        put("polycythaemia", "polycythemia");
        put("thrombocytopenia", "platelets");
        put("thrombopoiesis", "platelets thrombopoiesis");
        put("transfusion reaction", "blood transfusion");
        put("blood transfusion reaction", "blood transfusion");
        put("bone marrow biopsy", "bone marrow aspiration and biopsy");
        put("bone marrow aspiration", "bone marrow aspiration and biopsy");
        put("bone marrow transplant", "hematopoietic stem cell transplantation");
        put("stem cell transplant", "hematopoietic stem cell transplantation");
        put("folic acid", "folate");
        put("vitamin b12 deficiency", "vitamin b12");
        put("iron overload", "iron");
        put("iron studies", "iron");
        put("ebv", "epstein barr virus");
        put("epstein-barr virus", "epstein barr virus");
        put("parvovirus", "parvovirus b19");
        put("fifth disease", "parvovirus b19");
        put("q fever", "q fever coxiella burnetii");
        put("coxiella", "coxiella burnetii");
        put("typhus", "rickettsial typhus");
        put("enteric fever", "salmonella typhi enteric fever");
        put("typhoid", "salmonella typhi enteric fever");
        put("typhoid fever", "salmonella typhi enteric fever");
        put("plague", "yersinia pestis plague");
        put("brucellosis", "brucella");
        put("undulant fever", "brucella");
        put("malaria", "malaria");
        put("filariasis", "lymphatic filariasis");
        put("elephantiasis", "lymphatic filariasis");
        put("leishmaniasis", "leishmaniasis");
        put("kala azar", "visceral leishmaniasis");
        put("kala-azar", "visceral leishmaniasis");
        put("toxoplasmosis", "toxoplasmosis");
        put("trypanosomiasis", "african trypanosomiasis");
        put("sleeping sickness", "african trypanosomiasis");
        put("anemia in pregnancy", "nutritional anemias");
        put("microcytic anemia", "iron deficiency anemia");
        put("macrocytic anemia", "nutritional anemias");
        put("megaloblastic anemia", "nutritional anemias");

        // ---- Arabic surface forms (conceptual support for Arabic-speaking students) ----
        put("تحليل الدم", CBC);
        put("صورة الدم", CBC);
        put("صورة الدم الكاملة", CBC);
        put("تعداد الدم", CBC);
        put("تعداد الدم الكامل", CBC);
        put("تحليل الدم الشامل", CBC);
        put("الدم الكامل", CBC);
        put("الهيموغلوبين", HEMOGLOBIN);
        put("الخضاب", HEMOGLOBIN);
        put("خضاب الدم", HEMOGLOBIN);
        put("كريات الدم الحمراء", RBC);
        put("كرية الدم الحمراء", RBC);
        put("الكريات الحمراء", RBC);
        put("كريات الدم البيضاء", WBC);
        put("كرية الدم البيضاء", WBC);
        put("الكريات البيضاء", WBC);
        put("الصفائح الدموية", PLATELET);
        put("صفيحات الدم", PLATELET);
        put("الصفيحات", PLATELET);
        put("فقر الدم", "anemia");
        put("الانيميا", "anemia");
        put("فقر الدم بعوز الحديد", "iron deficiency anemia");
        put("فقر الدم بنقص الحديد", "iron deficiency anemia");
        put("الانيميا بعوز الحديد", "iron deficiency anemia");
        put("فقر الدم اللاتنسجي", "aplastic anemia");
        put("الانيميا اللاتنسجية", "aplastic anemia");
        put("فقر الدم المنجلي", "sickle cell anemia");
        put("الانيميا المنجلية", "sickle cell anemia");
        put("مرض الخلايا المنجلية", "sickle cell disease");
        put("الثلاسيميا", "thalassemia");
        put("فقر دم البحر المتوسط", "thalassemia");
        put("فقر الدم الانحلالي", "hemolytic anemia");
        put("الانيميا الانحلالية", "hemolytic anemia");
        put("ابيضاض الدم", "leukemia");
        put("اللوكيميا", "leukemia");
        put("سرطان الدم", "leukemia");
        put("نخاع العظم", "bone marrow");
        put("النخاع العظمي", "bone marrow");
        put("الطحال", "spleen");
        put("الغدة الزعترية", "thymus");
        put("الغدة التيموسية", "thymus");
        put("التيموس", "thymus");
        put("العقد اللمفية", "lymph nodes");
        put("الغدد اللمفاوية", "lymph nodes");
        put("العقد الليمفاوية", "lymph nodes");
        put("الأوعية اللمفية", "lymphatic vessels");
        put("الاوعية اللمفاوية", "lymphatic vessels");
        put("تكون الدم", "hematopoiesis");
        put("تكون الكريات الحمراء", "erythropoiesis");
        put("الارثروبويتين", "erythropoietin");
        put("الإرثروبويتين", "erythropoietin");
        put("تخثر الدم", "hemostasis coagulation");
        put("التجلط", "hemostasis coagulation");
        put("الارقاء", "hemostasis coagulation");
        put("الإرقاء", "hemostasis coagulation");
        put("الهيموفيليا", "hemophilia");
        put("الناعور", "hemophilia");
        put("نقص الجلوكوز 6 فوسفات", "glucose 6 phosphate dehydrogenase");
        put("الحمى المالطية", "brucella");
        put("داء المقوسات", "toxoplasmosis");
        put("داء المقوسات Toxoplasma", "toxoplasmosis");
        put("الليشمانيا", "leishmaniasis");
        put("الليشمانية", "leishmaniasis");
        put("الحبة الشرقية", "visceral leishmaniasis");
        put("الملاريا", "malaria");
        put("الطاعون", "yersinia pestis plague");
        put("التيفوئيد", "salmonella typhi enteric fever");
        put("حمى التيفود", "salmonella typhi enteric fever");
        put("التيفوس", "rickettsial typhus");
        put("نقل الدم", "blood transfusion");
        put("نقل الدم ومضاعفاته", "blood transfusion");
        put("فصائل الدم", "blood groups");
        put("زمر الدم", "blood groups");
        put("مجموعات الدم", "blood groups");
        put("حمض الفوليك", "folate");
        put("الفولات", "folate");
        put("فيتامين ب12", "vitamin b12");
        put("فيتامين ب 12", "vitamin b12");
        put("الحديد", "iron");
        put("المنجلية", "sickle cell anemia");
        put("داء الليشمانيات", "leishmaniasis");
        put("زرع النخاع", "hematopoietic stem cell transplantation");
        put("زراعة الخلايا الجذعية", "hematopoietic stem cell transplantation");
        put("الخلايا الجذعية", "hematopoietic stem cell transplantation");
        put("اللمفومة", "lymphoid tissue disorders");
        put("اعتلالات العقد اللمفية", "lymphoid tissue disorders");
        put("كثرة الحمر", "polycythemia");
        put("كثرة الحمر الحقيقية", "polycythemia");
        put("الأمراض النقوية التكاثرية", "myeloproliferative disorders");
        put("فقر الدم الغذائي", "nutritional anemias");
        put("العدوى المنقولة بالدم", "blood borne infections");
        put("الأمراض المنقولة بالدم", "blood borne infections");
    }

    /** Multi-word aliases, longest first, for greedy in-query phrase substitution. */
    private static final List<String> MULTI_WORD;
    /** Single-token aliases. */
    private static final Map<String, String> SINGLE_WORD;

    static {
        List<String> multi = new ArrayList<String>();
        Map<String, String> single = new HashMap<String, String>();
        for (Map.Entry<String, String> e : ALIASES.entrySet()) {
            if (e.getKey().indexOf(' ') >= 0) multi.add(e.getKey());
            else single.put(e.getKey(), e.getValue());
        }
        Collections.sort(multi, (a, b) -> Integer.compare(b.length(), a.length()));
        MULTI_WORD = Collections.unmodifiableList(multi);
        SINGLE_WORD = Collections.unmodifiableMap(single);
    }

    private MedicalLexicon() { }

    /** All known surface forms, for tests and diagnostics. */
    public static Map<String, String> aliases() {
        return Collections.unmodifiableMap(ALIASES);
    }

    /** Canonical form for an exact (whole-query) surface form, or {@code null}. */
    public static String canonicalFor(String normalizedQuery) {
        return ALIASES.get(normalizedQuery);
    }

    /** Canonical expansion for a single token, or {@code null}. */
    public static String canonicalToken(String token) {
        return SINGLE_WORD.get(token);
    }

    /**
     * Rewrites a normalised query by substituting known aliases with their canonical phrases.
     *
     * <p>Multi-word aliases are replaced first (longest first) so that {@code "full blood count"}
     * resolves as a unit, then remaining single-token aliases are expanded. Unknown words are
     * preserved verbatim so the lexical overlap scorer still sees them.</p>
     */
    public static String expand(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isEmpty()) return "";
        String working = " " + normalizedQuery + " ";
        for (String alias : MULTI_WORD) {
            String canonical = ALIASES.get(alias);
            working = working.replace(" " + alias + " ", " " + canonical + " ");
        }
        StringBuilder out = new StringBuilder();
        for (String token : working.trim().split(" ")) {
            if (token.isEmpty()) continue;
            String canonical = SINGLE_WORD.get(token);
            if (out.length() > 0) out.append(' ');
            out.append(canonical != null ? canonical : token);
        }
        return out.toString();
    }

    /**
     * True when the whole query is itself a known alias (e.g. a bare acronym such as {@code CBC}).
     */
    public static boolean isAliasQuery(String normalizedQuery) {
        return ALIASES.containsKey(normalizedQuery);
    }
}
