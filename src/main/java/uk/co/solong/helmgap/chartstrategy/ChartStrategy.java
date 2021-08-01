package uk.co.solong.helmgap.chartstrategy;

import uk.co.solong.helmgap.ExternalProcessError;
import uk.co.solong.helmgap.HelmPullException;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;

import java.io.File;
import java.nio.file.Path;

public interface ChartStrategy {
    File getChartArchive(ChartDescriptor chartDescriptor, Path pullDir, Path chartTarTmp) throws HelmPullException, ExternalProcessError;
}
