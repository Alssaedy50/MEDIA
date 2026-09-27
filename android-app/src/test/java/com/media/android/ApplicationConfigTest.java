package com.media.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApplicationConfigTest {
    @Test
    public void packageAndVersionMatchTheOfflineAlphaRelease() {
        assertEquals("com.media.android", BuildConfig.APPLICATION_ID);
        assertEquals("0.3.0", BuildConfig.VERSION_NAME);
    }

    @Test
    public void debugBuildIsNotMinified() {
        assertTrue(BuildConfig.DEBUG);
        assertFalse(BuildConfig.APPLICATION_ID.isEmpty());
    }
}
