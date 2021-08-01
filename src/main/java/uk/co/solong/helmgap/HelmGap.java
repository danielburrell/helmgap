package uk.co.solong.helmgap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;
import uk.co.solong.helmgap.helm.Helm;
import uk.co.solong.helmgap.kbld.Kbld;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class HelmGap {
    static final Logger logger = LoggerFactory.getLogger(HelmGap.class);
    static final String PULL_DIR = "pull";
    static final String TEMPLATE_DIR = "template";
    static final String LOCK_FILE_DIR = "lockfile";
    static final String ARCHIVE_DIR = "archive";
    static final String CHART_TAR_TMP = "chartTartTmp";
    public final Kbld kbld;
    public final Helm helm;

    public HelmGap() {
        this("helm", "kbld");
    }

    public HelmGap(String helm, String kbld) {
        this.kbld = new Kbld(kbld);
        this.helm = new Helm(helm);
    }
    /**
     * Builds an airgap install by generating an AirgapInstall object representing a
     * tar of the registry images required by the helm chart, as well as the helm chart.
     *
     * @param chartDescriptor the chart descriptor obtainable through ChartDescriptor.by methods.
     * @return AirgapInstall representing the airgap registry and chart files.
     * @throws AirgapInstallException for any AirgapInstallExceptions that might be thrown.
     */
    public AirgapInstall buildAirgap(ChartDescriptor chartDescriptor) throws AirgapInstallException {
        return buildAirgap(chartDescriptor, new HashMap<>());
    }

    /**
     * Builds an airgap install by generating an AirgapInstall object representing a
     * tar of the registry images required by the helm chart, as well as the helm chart.
     *
     * @param chartDescriptor the chart descriptor obtainable through ChartDescriptor.by methods.
     * @param valueOverrides  a map of chart override values equivalent to helm --set key=value
     * @return AirgapInstall representing the airgap registry and chart files.
     * @throws AirgapInstallException for any AirgapInstallExceptions that might be thrown.
     */
    public AirgapInstall buildAirgap(ChartDescriptor chartDescriptor, Map<String, String> valueOverrides) throws AirgapInstallException {
        verifyLinux();
        Path pullDir;
        Path chartTarTmp;
        Path chartRoot;
        Path templateDir;
        Path lockFileDir;
        Path archiveDir;
        try {
            Path sessionRoot = Files.createTempDirectory("helmgap");
            pullDir = Paths.get(sessionRoot.toString(), PULL_DIR);
            templateDir = Paths.get(sessionRoot.toString(), TEMPLATE_DIR);
            lockFileDir = Paths.get(sessionRoot.toString(), LOCK_FILE_DIR);
            archiveDir = Paths.get(sessionRoot.toString(), ARCHIVE_DIR);
            chartTarTmp = Paths.get(sessionRoot.toString(), CHART_TAR_TMP);
            initDirectories(pullDir, templateDir, lockFileDir, archiveDir, chartTarTmp);
        } catch (IOException e) {
            throw new WorkspaceSetupException("Could not create a temporary workspace", e);
        }

        File chartArchive = chartDescriptor.getChartStrategy().getChartArchive(chartDescriptor, pullDir, chartTarTmp);
        chartRoot = determineChartRoot(pullDir);
        ChartMetadata chartMetadata = determineChartMetadata(chartRoot);
        helm.helmDeleteTests(chartRoot);
        helm.helmTemplate(chartRoot, templateDir, valueOverrides);
        File lockFile = kbld.generateLockFile(templateDir, lockFileDir, chartMetadata.getName(), chartMetadata.getVersion());
        File imageArchive = kbld.pkg(lockFile, archiveDir, chartMetadata.getName(), chartMetadata.getVersion());

        return new AirgapInstall(imageArchive, chartArchive, lockFile);
    }

    private ChartMetadata determineChartMetadata(Path chartRoot) throws MalformedChartException {
        File file = Paths.get(chartRoot.toString(), "Chart.yaml").toFile();
        if (file.exists()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
                return objectMapper.readValue(file, ChartMetadata.class);
            } catch (IOException e) {
                throw new MalformedChartException("Chart file could not be parsed: " + file.toString(), e);
            }
        } else {
            throw new MalformedChartException("Chart does not contain Chart.yaml file : " + file.toString());
        }
    }

    private Path determineChartRoot(Path pullDir) throws FailedToDetermineChartRootException {
        try {
            return Files.list(pullDir).filter(x -> x.toFile().isDirectory()).findFirst().orElseThrow(FailedToDetermineChartRootException::new);
        } catch (IOException e) {
            throw new FailedToDetermineChartRootException(e);
        }
    }

    private void verifyLinux() throws IncompatibleOperatingSystemException {
        if (!SystemUtils.IS_OS_UNIX) {
            throw new IncompatibleOperatingSystemException(SystemUtils.OS_NAME);
        }
    }

    private void initDirectories(Path... dirs) {
        Stream.of(dirs).forEach(x -> x.toFile().mkdirs());
    }

}
