package uk.co.solong.helmgap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelmGap {
    static final Logger logger = LoggerFactory.getLogger(HelmGap.class);
    static final String PULL_DIR = "pull";
    static final String TEMPLATE_DIR = "template";
    static final String SHA_DIR = "sha";
    static final String ARCHIVE_DIR = "archive";
    static final String CHART_TAR_TMP = "chartTartTmp";
    public final String kbld;
    public final String helm;


    public HelmGap() {
        this("helm", "kbld");
    }

    public HelmGap(String helm, String kbld) {
        this.helm = helm;
        this.kbld = kbld;
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

        File chartArchive = helmPull(chartDescriptor, pullDir, chartTarTmp);
        chartRoot = determineChartRoot(pullDir);
        ChartMetadata chartMetadata = determineChartMetadata(chartRoot);
        helmDeleteTests(chartRoot);
        helmTemplate(chartRoot, templateDir, valueOverrides);
        File lockFile = kbldToLockFile(templateDir, shaDir);
        File imageArchive = kbldPkg(lockFile, archiveDir, chartMetadata.getName(), chartMetadata.getVersion());

        return new AirgapInstall(imageArchive, chartArchive, lockFile);
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

    File kbldPkg(File shaFile, Path archiveDir, String name, String version) throws KbldPkgException, ExternalProcessError {
        logger.info("Generating Package Archive");
        ProcessBuilder builder = new ProcessBuilder();
        File archiveFile = Paths.get(archiveDir.toString(), name + "-airgap-" + version + ".tgz").toFile();

        builder.command(kbld, "pkg", "-f", shaFile.toString(), "-o", archiveFile.toString());
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

    File kbldToLockFile(Path templateDirectory, Path shaDir) throws ExternalProcessError, KbldLockFileGenerationException {
        String output;
        try {
            File lockFile = Paths.get(shaDir.toString(), "sha256").toFile();
            output = new ProcessExecutor().command(kbld, "-f", templateDirectory.toAbsolutePath().toString(), "--lock-output", lockFile.getAbsolutePath())
                    .exitValues(0).readOutput(true)
                    .redirectOutput(Slf4jStream.of(getClass()).asDebug())
                    .redirectError(Slf4jStream.of(getClass()).asInfo())
                    .execute().outputUTF8();
            return lockFile;
        }
        catch (InvalidExitValueException e) {
            logger.error("The lockfile generator (kbld) has exited with error code {}. See stderr above for more information", e.getExitValue());
            throw new KbldLockFileGenerationException("An error has occurred while generating the lockfile via kbld. Check the logs for more info...", e);
        } catch (InterruptedException | IOException | TimeoutException e) {
            throw new ExternalProcessError(e);
        }
    }

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

    void helmTemplate(Path chartDir, Path templateDir, Map<String, String> valueOverrides) throws HelmTemplateException, ExternalProcessError {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> command = generateCommand(templateDir, valueOverrides);
        builder.command(command);
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

    private List<String> generateCommand(Path templateDir, Map<String, String> valueOverrides) {
        List<String> overrideKeyPairParams = valueOverrides.entrySet().stream().map(x -> x.getKey() + "=" + x.getValue()).collect(Collectors.toList());
        List<String> command = new ArrayList<>();
        command.add(helm);
        command.add("template");
        for (String overrideKeyPairParam : overrideKeyPairParams) {
            command.add("--set");
            command.add(overrideKeyPairParam);
        }
        command.add("--output-dir");
        command.add(templateDir.toString());
        command.add(".");
        return command;
    }


    File helmPull(ChartDescriptor chartDescriptor, Path pullDir, Path chartTarTmp) throws ExternalProcessError, HelmPullException {

        //download and untar
        {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(mergeChartCommand(chartDescriptor, helm, "pull", "--untar"));
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
                //clean up the one and only empty directory that helm leaves behind
                DirectoryStream<Path> paths = Files.newDirectoryStream(pullDir.toAbsolutePath());
                for (Path path : paths) {
                    if (isEmpty(path)) {
                        Files.delete(path);
                        break;
                    }
                }
            } catch (IOException | InterruptedException | HelmPullException e) {
                throw new ExternalProcessError(e);
            }
        }

        //download the archive file
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(mergeChartCommand(chartDescriptor, helm, "pull", "--destination", chartTarTmp.toString()));
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

    public boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }

        return false;
    }

    private String[] mergeChartCommand(ChartDescriptor suffix, String... param) {
        List<String> command = new ArrayList<>();
        Collections.addAll(command, param);
        command.addAll(suffix.getDescriptor());
        return command.toArray(new String[0]);
    }

}
