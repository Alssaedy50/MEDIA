package com.media.android.knowledge;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction over a hierarchical read-only store of knowledge text.
 *
 * <p>The application reads the bundled records from Android {@code assets}; pure-JVM tests read the
 * very same files from the repository tree. Keeping this behind an interface means retrieval logic
 * is exercised against the real corpus during unit tests without Robolectric.</p>
 */
public interface KnowledgeSource {

    /** Names of files and directories directly under {@code path} (empty when absent). */
    List<String> list(String path) throws IOException;

    /** UTF-8 text content of a file. */
    String read(String path) throws IOException;
}
