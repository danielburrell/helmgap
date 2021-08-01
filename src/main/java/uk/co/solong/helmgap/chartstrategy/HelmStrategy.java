package uk.co.solong.helmgap.chartstrategy;

import uk.co.solong.helmgap.ExternalProcessError;
import uk.co.solong.helmgap.HelmPullException;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;
import uk.co.solong.helmgap.helm.Helm;

import java.io.File;
import java.nio.file.Path;

public class HelmStrategy implements ChartStrategy{
    //FIXME: should be singleton injected
    private Helm helm = new Helm("helm");
    @Override
    public File getChartArchive(ChartDescriptor chartDescriptor, Path pullDir, Path chartTarTmp) throws HelmPullException, ExternalProcessError {
        return helm.helmPull(chartDescriptor, pullDir, chartTarTmp);
    }
}
