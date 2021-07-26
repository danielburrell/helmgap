package uk.co.solong.helmgap.descriptors;

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

}
