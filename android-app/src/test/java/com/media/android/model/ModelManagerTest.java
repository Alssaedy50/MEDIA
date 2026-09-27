package com.media.android.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;

/** Truthfulness tests for the model manager: it must never claim a model is ready. */
public class ModelManagerTest {

    @Test
    public void reportsNotInstalledWhenNoArtifactExists() {
        File dir = new File(System.getProperty("java.io.tmpdir"),
                "media-model-" + System.nanoTime());
        assertTrue(dir.mkdirs() || dir.isDirectory());
        ModelManager manager = new ModelManager(dir);
        assertEquals(ModelManager.State.NOT_INSTALLED, manager.state());
        assertFalse(manager.isReady());
        assertEquals("Not installed", manager.statusLabel());
        assertEquals("—", manager.formattedSize());
    }

    @Test
    public void expectedFoundationParametersMatchTheFrozenSpec() {
        ModelManager.Expectation e = ModelManager.FOUNDATION_EXPECTATION;
        assertEquals("media-100m-foundation-v1", e.name);
        assertEquals("GGUF", e.format);
        assertEquals("Q4_K_M", e.quantization);
        assertEquals(111_365_632L, e.parameters);
        assertEquals(16_384, e.vocabulary);
        assertEquals(256, e.contextLength);
    }

    @Test
    public void presenceOfAnUnverifiedArtifactIsReportedAsNotReady() {
        File dir = new File(System.getProperty("java.io.tmpdir"),
                "media-model-" + System.nanoTime());
        File models = new File(dir, "models");
        assertTrue(models.mkdirs() || models.isDirectory());
        // A file with the expected name but no verified runtime must not be reported as Ready.
        File fake = new File(models, "media-100m-foundation-v1-q4_k_m.gguf");
        try {
            java.nio.file.Files.write(fake.toPath(), new byte[]{1, 2, 3});
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        ModelManager manager = new ModelManager(dir);
        assertFalse("an unverified artifact must never be reported as ready", manager.isReady());
        assertEquals(ModelManager.State.INVALID, manager.state());
    }
}
