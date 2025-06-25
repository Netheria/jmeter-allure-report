package com.allure.allure_report_context;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods to write attachments on disk under the report path.
 */
public class AllureIO {

    /**
     * Writes the given text content to a file named by "source" under the results directory.
     *
     * @param resultsDir  the results folder (e.g. from vars.get("ALLURE_REPORT_PATH"))
     * @param source      the attachment source ID, e.g. "caseUuid-attachmentUuid-..."
     * @param content     the raw text to write
     * @throws IOException if any I/O error occurs
     */
    public static void writeAttachment(String resultsDir, String source, String content) throws IOException {
        Path target = Paths.get(resultsDir, source);
        Files.createDirectories(target.getParent());
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
    }
}
