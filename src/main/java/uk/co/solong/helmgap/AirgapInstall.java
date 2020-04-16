package uk.co.solong.helmgap;

import java.io.File;

public class AirgapInstall {
    private final File registryArchive;
    private final File chartPullArchive;

    AirgapInstall(File registryArchive, File chartPullArchive) {
        this.registryArchive = registryArchive;
        this.chartPullArchive = chartPullArchive;
    }

    /**
     * Returns a copy of the original chart that was requested, as a file reference.
     * @return - a copy of the original chart.
     */
    public File getOriginalChart() {
        return chartPullArchive;
    }

    /**
     * Returns the airgap installer archive, as a file reference.
     * @return - the airgap installer archive.
     */
    public File getAirgapInstallerArchive() {
        return registryArchive;
    }
}
