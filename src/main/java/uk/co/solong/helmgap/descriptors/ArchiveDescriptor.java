package uk.co.solong.helmgap.descriptors;

import uk.co.solong.helmgap.chartstrategy.ChartStrategy;
import uk.co.solong.helmgap.chartstrategy.TarStrategy;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArchiveDescriptor extends ChartDescriptor{
    private final File archiveFile;

    public ArchiveDescriptor(File archiveFile) {
        this.archiveFile = archiveFile;
    }

    @Override
    public List<String> getDescriptor() {
        return Collections.singletonList(archiveFile.getAbsolutePath());
    }

    @Override
    public ChartStrategy getChartStrategy() {
        return new TarStrategy();
    }
}
