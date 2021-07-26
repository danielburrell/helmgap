package uk.co.solong.helmgap;

import java.io.File;

public class AirgapInstall {
    private final File imageArchive;
    private final File chartPullArchive;
    private final File kbldLockFile;

    AirgapInstall(File imageArchive, File chartArchive, File kbldLockFile) {
        this.imageArchive = imageArchive;
        this.chartPullArchive = chartArchive;
        this.kbldLockFile = kbldLockFile;
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
        return imageArchive;
    }

    /**
     * Returns the kbld lockfile, as a file reference.
     * @return - the kbld lock file
     */
    public File getLockFile() {
        return kbldLockFile;
    }
}
