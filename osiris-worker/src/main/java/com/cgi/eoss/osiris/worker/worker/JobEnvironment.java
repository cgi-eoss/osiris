package com.cgi.eoss.osiris.worker.worker;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * <p>Describes the workspace for an OSIRIS job execution.</p>
 */
@Data
@Builder
class JobEnvironment {
    /**
     * <p>The identifier of the job using this environment.</p>
     */
    private String jobId;

    /**
     * <p>The in-process workspace, used for temporary files created during service execution.</p>
     */
    private Path workingDir;

    /**
     * <p>The input workspace, pre-populated by the {@link OsirisWorker} before service execution.</p>
     */
    private Path inputDir;

    /**
     * <p>The output workspace, used for end-result files created during service execution. The {@link OsirisWorker}
     * collects from this path.</p>
     */
    private Path outputDir;
}
