package uk.co.solong.helmgap.chartstrategy;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import uk.co.solong.helmgap.ExternalProcessError;
import uk.co.solong.helmgap.HelmPullException;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

public class TarStrategy implements ChartStrategy{
    private String tar = "tar";

    @Override
    public File getChartArchive(ChartDescriptor chartDescriptor, Path pullDir, Path chartTarTmp) throws HelmPullException, ExternalProcessError {
        //untar the file located in the chartDescriptor into the pullDir so that you have pullDir/redis/Chart.yml
        File chartFile = new File(chartDescriptor.getDescriptor().get(0));

        try {
            String output = new ProcessExecutor().command(tar, "-C", pullDir.toString(), "-xvf", chartFile.toString())
                    .exitValues(0).readOutput(true)
                    .redirectOutput(Slf4jStream.of(getClass()).asDebug())
                    .redirectError(Slf4jStream.of(getClass()).asInfo())
                    .execute().outputUTF8();
        } catch (InterruptedException | TimeoutException | IOException e) {
            throw new ExternalProcessError();
        }
        //return the chart
        return chartFile;
    }
}
