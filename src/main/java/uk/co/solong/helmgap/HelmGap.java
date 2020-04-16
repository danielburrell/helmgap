package uk.co.solong.helmgap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class HelmGap {
    static final Logger logger = LoggerFactory.getLogger(HelmGap.class);
    static final String PULL_DIR = "pull";
    static final String TEMPLATE_DIR = "template";
    static final String SHA_DIR = "sha";
    static final String ARCHIVE_DIR = "archive";
    static final String CHART_TAR_TMP = "chartTartTmp";


    /**
     * Builds an airgap install by generating an AirgapInstall object representing a
     * tar of the registry images required by the helm chart, as well as the helm chart.
     *
     * @param chartDescriptor the chart descriptor obtainable through ChartDescriptor.by methods..
     * @return AirgapInstall representing the airgap registry and chart files.
     * @throws IOException          if there's an issue reading or writing to disk.
     * @throws InterruptedException if the library is interrupted waiting for an external process to complete.
     */
    public AirgapInstall buildAirgap(ChartDescriptor chartDescriptor) throws AirgapInstallException {
        verifyLinux();
        Path pullDir;
        Path chartTarTmp;
        Path chartRoot;
        Path templateDir;
        Path shaDir;
        Path archiveDir;
        try {
            Path sessionRoot = Files.createTempDirectory("helmgap");
            pullDir = Paths.get(sessionRoot.toString(), PULL_DIR);
            templateDir = Paths.get(sessionRoot.toString(), TEMPLATE_DIR);
            shaDir = Paths.get(sessionRoot.toString(), SHA_DIR);
            archiveDir = Paths.get(sessionRoot.toString(), ARCHIVE_DIR);
            chartTarTmp = Paths.get(sessionRoot.toString(), CHART_TAR_TMP);
            initDirectories(pullDir, templateDir, shaDir, archiveDir, chartTarTmp);
        } catch (IOException e) {
            throw new WorkspaceSetupException("Could not create a temporary workspace", e);
        }

        File chartPullArchive = helmPull(chartDescriptor, pullDir, chartTarTmp);
        chartRoot = determineChartRoot(pullDir);
        ChartMetadata chartMetadata = determineChartMetadata(chartRoot);
        helmDeleteTests(chartRoot);
        helmTemplate(chartRoot, templateDir);
        File shaFile = kbldToSha(templateDir, shaDir);
        File archiveFile = kbldPkg(shaFile, archiveDir, chartMetadata.getName(), chartMetadata.getVersion());

        return new AirgapInstall(archiveFile, chartPullArchive);
    }

    private ChartMetadata determineChartMetadata(Path chartRoot) throws MalformedChartException {
        File file = Paths.get(chartRoot.toString(), "Chart.yaml").toFile();
        if (file.exists()) {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            try {
                JsonNode jsonNode = objectMapper.readTree(file);
                String name = jsonNode.get("name").textValue();
                String version = jsonNode.get("version").textValue();
                return new ChartMetadata(name, version);
            } catch (IOException e) {
                throw new MalformedChartException("Chart file could not be parsed: "+file.toString(), e);
            }
        } else {
            throw new MalformedChartException("Chart does not contain Chart.yaml file : "+ file.toString());
        }
    }

    private Path determineChartRoot(Path pullDir) throws FailedToDetermineChartRootException {
        try {
            return Files.list(pullDir).findFirst().orElseThrow(FailedToDetermineChartRootException::new);
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

    File kbldPkg(File shaFile, Path archiveDir, String name, String version) throws KbldPkgException, ExternalProcessError {
        logger.info("Generating Package Archive");
        ProcessBuilder builder = new ProcessBuilder();
        File archiveFile = Paths.get(archiveDir.toString(), name + "-airgap-" + version + ".tgz").toFile();

        builder.command("kbld", "pkg", "-f", shaFile.toString(), "-o", archiveFile.toString());
        builder.directory(archiveDir.toFile());
        Process process;
        try {
            process = builder.start();
            String helmOutput = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.debug(helmOutput);
            } else {
                throw new KbldPkgException(helmOutput);
            }
        } catch (InterruptedException | IOException e) {
            throw new ExternalProcessError(e);
        }
        return archiveFile;
    }

    File kbldToSha(Path templateDirectory, Path shaDir) throws KbldShaException, ExternalProcessError {
        logger.info("Generating SHA256 manifest");
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("kbld", "-f", ".");
        builder.directory(templateDirectory.toFile());
        File shaFile = Paths.get(shaDir.toString(), "sha256").toFile();
        builder.redirectOutput(shaFile);
        Process process;
        try {
            process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.debug("Sha256 manifest generation successful");
            } else {
                throw new KbldShaException();
            }
        } catch (InterruptedException | IOException e) {
            throw new ExternalProcessError(e);
        }
        return shaFile;
    }

    /**
     * Recursively deletes any folder and its contents if the folder is called tests
     *
     * @param chartRoot the root of the chart e.g. /tmp/pullroot/stackstorm-ha/
     * @throws IOException
     */
    void helmDeleteTests(Path chartRoot) throws CouldNotDeleteTestsException {
        logger.info("Deleting Test Directories");

        //recursively delete any **tests** folder
        try {
            Files.walkFileTree(chartRoot, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if ("tests".equals(dir.toFile().getName())) {

                        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult postVisitDirectory(Path subdirectory, IOException exc) throws IOException {
                                Files.delete(subdirectory);
                                logger.debug("Deleting {}", subdirectory);
                                return FileVisitResult.CONTINUE;
                            }

                            ;

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                logger.debug("Deleting {}", file);
                                return FileVisitResult.CONTINUE;
                            }

                        });

                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (IOException e) {
            throw new CouldNotDeleteTestsException(e);
        }
    }

    void helmTemplate(Path chartDir, Path templateDir) throws HelmTemplateException, ExternalProcessError {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("helm", "template", "--output-dir", templateDir.toString(), ".");
        logger.info("Generating interpolated manifest with helm template: {}", String.join(" ", builder.command()));
        builder.directory(chartDir.toFile());
        Process process;
        try {
            process = builder.start();
            process.getErrorStream().transferTo(Slf4jStream.of(logger).asError());
            process.getInputStream().transferTo(Slf4jStream.of(logger).asDebug());
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.debug("Template generation successful");
            } else {
                throw new HelmTemplateException();
            }
        } catch (InterruptedException | IOException e) {
            throw new ExternalProcessError(e);
        }
    }


    File helmPull(ChartDescriptor chartDescriptor, Path pullDir, Path chartTarTmp) throws ExternalProcessError, HelmPullException {

        {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(mergeChartCommand(chartDescriptor, "helm", "pull", "--untar"));
            logger.info("Pulling untar helm chart : {}", String.join(" ", pb.command()));
            pb.directory(pullDir.toFile());
            Process process;
            try {
                process = pb.start();
                process.getErrorStream().transferTo(Slf4jStream.of(logger).asError());
                process.getInputStream().transferTo(Slf4jStream.of(logger).asDebug());
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new HelmPullException();
                }
            } catch (IOException | InterruptedException | HelmPullException e) {
                throw new ExternalProcessError(e);
            }
        }
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(mergeChartCommand(chartDescriptor, "helm", "pull", "--destination", chartTarTmp.toString()));
            logger.info("Pulling archive helm chart : {}", String.join(" ", builder.command()));
            builder.directory(pullDir.toFile());
            Process process;
            try {
                process = builder.start();
                process.getErrorStream().transferTo(Slf4jStream.of(logger).asError());
                process.getInputStream().transferTo(Slf4jStream.of(logger).asDebug());
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    File archive = Files.list(chartTarTmp).findFirst().get().toFile();
                    logger.info(archive.toString());
                    return archive;
                } else {
                    throw new HelmPullException();
                }
            } catch (InterruptedException | IOException e) {
                throw new ExternalProcessError(e);
            }
        }
    }

    private String[] mergeChartCommand(ChartDescriptor suffix, String... param) {
        List<String> command = new ArrayList<>();
        Collections.addAll(command, param);
        command.addAll(suffix.getDescriptor());
        return command.toArray(new String[0]);
    }

}
