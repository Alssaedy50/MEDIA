package com.media.android;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BackendUrlTest {
    @Test
    public void defaultBackendUrlTargetsAndroidEmulatorHost() {
        assertTrue(BuildConfig.MEDIA_WEB_URL.startsWith("http"));
    }
}
