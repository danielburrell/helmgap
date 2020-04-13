package uk.co.solong.helmgap;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
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
     * @throws IOException
     * @throws InterruptedException
     */
    public AirgapInstall buildAirgap(ChartDescriptor chartDescriptor) throws IOException, InterruptedException {
        Path sessionRoot = Files.createTempDirectory("helmgap");
        Path pullDir = Paths.get(sessionRoot.toString(), PULL_DIR);
        Path templateDir = Paths.get(sessionRoot.toString(), TEMPLATE_DIR);
        Path shaDir = Paths.get(sessionRoot.toString(), SHA_DIR);
        Path archiveDir = Paths.get(sessionRoot.toString(), ARCHIVE_DIR);
        Path chartRoot = Paths.get(pullDir.toString(), chartDescriptor.getName());
        Path chartTarTmp = Paths.get(sessionRoot.toString(), CHART_TAR_TMP);

        initDirectories(pullDir, templateDir, shaDir, archiveDir, chartTarTmp);

        File chartPullArchive = helmPull(chartDescriptor, pullDir, chartTarTmp);
        helmDeleteTests(chartRoot);
        helmTemplate(chartRoot, templateDir);
        File shaFile = kbldToSha(templateDir, shaDir);
        File archiveFile = kbldPkg(shaFile, archiveDir, chartDescriptor.getName(), chartDescriptor.getVersion());

        return new AirgapInstall(archiveFile, chartPullArchive);
    }

    private void initDirectories(Path... dirs) {
        Stream.of(dirs).forEach(x -> x.toFile().mkdirs());
    }

    File kbldPkg(File shaFile, Path archiveDir, String name, String version) throws IOException, InterruptedException {
        logger.info("Generating Package Archive");
        ProcessBuilder builder = new ProcessBuilder();
        File archiveFile = Paths.get(archiveDir.toString(),name+"-airgap-"+version+".tgz").toFile();

        builder.command("kbld", "pkg", "-f", shaFile.toString(), "-o", archiveFile.toString());
        builder.directory(archiveDir.toFile());
        Process process = builder.start();
        String helmOutput = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            logger.debug(helmOutput);
        } else {
            throw new KbldPkgException(helmOutput);
        }
        return archiveFile;
    }

    File kbldToSha(Path templateDirectory, Path shaDir) throws IOException, InterruptedException {
        logger.info("Generating SHA256 manifest");
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("kbld", "-f", ".");
        builder.directory(templateDirectory.toFile());
        File shaFile = Paths.get(shaDir.toString(),"sha256").toFile();
        builder.redirectOutput(shaFile);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            logger.debug("Sha256 manifest generation successful");
        } else {
            throw new KbldShaException();
        }
        return shaFile;
    }

    /**
     * Recursively deletes any folder and its contents if the folder is called tests
     * @param chartRoot the root of the chart e.g. /tmp/pullroot/stackstorm-ha/
     * @throws IOException
     */
    void helmDeleteTests(Path chartRoot) throws IOException {
        logger.info("Deleting Test Directories");

        //recursively delete any **tests** folder
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
                        };

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
    }

    void helmTemplate(Path chartDir, Path templateDir) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("helm", "template", "--output-dir", templateDir.toString(), ".");
        logger.info("Generating interpolated manifest with helm template: {}", String.join(" ", builder.command()));
        builder.directory(chartDir.toFile());
        Process process = builder.start();
        process.getErrorStream().transferTo(Slf4jStream.of(logger).asError());
        process.getInputStream().transferTo(Slf4jStream.of(logger).asDebug());
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            logger.debug("Template generation successful");
        } else {
            throw new HelmTemplateException();
        }
    }


    File helmPull(ChartDescriptor chartDescriptor, Path pullDir, Path chartTarTmp) throws IOException, InterruptedException {

        {
            ProcessBuilder untarBuilder = new ProcessBuilder();
            untarBuilder.command(mergeChartCommand(chartDescriptor, "helm", "pull", "--untar"));
            logger.info("Pulling untar helm chart {} : {}", chartDescriptor.getName(), String.join(" ", untarBuilder.command()));
            untarBuilder.directory(pullDir.toFile());
            Process process = untarBuilder.start();
            process.getErrorStream().transferTo(Slf4jStream.of(logger).asError());
            process.getInputStream().transferTo(Slf4jStream.of(logger).asDebug());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new HelmPullException();
            }
        }
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(mergeChartCommand(chartDescriptor, "helm", "pull", "--destination", chartTarTmp.toString()));
            logger.info("Pulling archive helm chart {} : {}", chartDescriptor.getName(), String.join(" ", builder.command()));
            builder.directory(pullDir.toFile());
            Process process = builder.start();
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
        }
    }

    private String[] mergeChartCommand(ChartDescriptor suffix, String... param) {
        List<String> command = new ArrayList<>();
        Collections.addAll(command, param);
        command.addAll(suffix.getDescriptor());
        return command.toArray(new String[0]);
    }

}
