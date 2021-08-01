package uk.co.solong.helmgap.helm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import uk.co.solong.helmgap.CouldNotDeleteTestsException;
import uk.co.solong.helmgap.ExternalProcessError;
import uk.co.solong.helmgap.HelmPullException;
import uk.co.solong.helmgap.HelmTemplateException;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Helm {
    private static final Logger logger = LoggerFactory.getLogger(Helm.class);
    private final String helm;

    public Helm(String helm) {
        this.helm = helm;
    }

    public File helmPull(ChartDescriptor chartDescriptor, Path pullDir, Path chartTarTmp) throws ExternalProcessError, HelmPullException {

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



    public void helmDeleteTests(Path chartRoot) throws CouldNotDeleteTestsException {
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

    public void helmTemplate(Path chartDir, Path templateDir, Map<String, String> valueOverrides) throws HelmTemplateException, ExternalProcessError {
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

    private String[] mergeChartCommand(ChartDescriptor suffix, String... param) {
        List<String> command = new ArrayList<>();
        Collections.addAll(command, param);
        command.addAll(suffix.getDescriptor());
        return command.toArray(new String[0]);
    }

    private boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }

        return false;
    }
}
