package uk.co.solong.helmgap;

import java.io.File;

public class AirgapInstall {
    private final File registryArchive;
    private final File chartPullArchive;

    AirgapInstall(File registryArchive, File chartPullArchive) {
        this.registryArchive = registryArchive;
        this.chartPullArchive = chartPullArchive;
    }

    public File getChartPullArchive() {
        return chartPullArchive;
    }

    public File getRegistryArchive() {
        return registryArchive;
    }
}
