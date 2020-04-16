package uk.co.solong.helmgap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.solong.helmgap.descriptors.ChartDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static uk.co.solong.helmgap.HelmGap.*;

class HelmGapTest {

    private Path sessionRoot;
    private Path pullDir;
    private Path templateDir;
    private Path shaDir;
    private Path archiveDir;
    private Path chartRoot;
    private String name;
    private Path chartTarTmp;
    private String repo;
    private String version;

    @BeforeAll
    public static void setupHelm() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder().inheritIO();
        builder.command("helm", "repo", "add", "stable", "https://kubernetes-charts.storage.googleapis.com");
        Process process = builder.start();
        int exitCode = process.waitFor();
    }
    @BeforeEach
    public void setupDirectories() throws IOException {
        name = "hackmd";
        repo = "stable";
        version = "0.1.0";
        sessionRoot = Files.createTempDirectory("helmgap");
        pullDir = Paths.get(sessionRoot.toString(), PULL_DIR);
        templateDir = Paths.get(sessionRoot.toString(), TEMPLATE_DIR);
        shaDir = Paths.get(sessionRoot.toString(), SHA_DIR);
        archiveDir = Paths.get(sessionRoot.toString(), ARCHIVE_DIR);
        chartTarTmp = Paths.get(sessionRoot.toString(), CHART_TAR_TMP);
        chartRoot = Paths.get(pullDir.toString(), name);


        pullDir.toFile().mkdirs();
        templateDir.toFile().mkdirs();
        shaDir.toFile().mkdirs();
        archiveDir.toFile().mkdirs();
        chartTarTmp.toFile().mkdirs();
    }
    @Test
    void helmPull() throws IOException, HelmPullException, ExternalProcessError {
        HelmGap testSubject = new HelmGap();
        assertEquals(0, Files.list(pullDir).count());
        ChartDescriptor chartDescriptor = ChartDescriptor.byShortName(repo, name, version);
        testSubject.helmPull(chartDescriptor, pullDir, chartTarTmp);
        assertEquals(1, Files.list(pullDir).count());
        assertTrue(chartRoot.toFile().exists());
    }

    @Test
    void helmDeleteTests() throws IOException, HelmPullException, ExternalProcessError, CouldNotDeleteTestsException {
        HelmGap testSubject = new HelmGap();
        ChartDescriptor chartDescriptor = ChartDescriptor.byShortName(repo, name, version);
        testSubject.helmPull(chartDescriptor, pullDir, chartTarTmp);
        testSubject.helmDeleteTests(chartRoot);

        Files.walkFileTree(chartRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if ("tests".equals(dir.toFile().getName())) {
                        fail("Chart still contains tests directory");
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
    }

    @Test
    void helmTemplate() throws HelmPullException, ExternalProcessError, CouldNotDeleteTestsException, HelmTemplateException {
        HelmGap testSubject = new HelmGap();
        ChartDescriptor chartDescriptor = ChartDescriptor.byShortName(repo, name, version);
        testSubject.helmPull(chartDescriptor, pullDir, chartTarTmp);
        testSubject.helmDeleteTests(chartRoot);
        testSubject.helmTemplate(chartRoot, templateDir);
        System.out.println(templateDir.toString());
    }

    @Test
    void kbldSha() throws IOException, HelmPullException, ExternalProcessError, CouldNotDeleteTestsException, HelmTemplateException, KbldShaException {
        HelmGap testSubject = new HelmGap();
        ChartDescriptor chartDescriptor = ChartDescriptor.byShortName(repo, name, version);
        testSubject.helmPull(chartDescriptor, pullDir, chartTarTmp);
        testSubject.helmDeleteTests(chartRoot);
        testSubject.helmTemplate(chartRoot, templateDir);
        File shaFile = testSubject.kbldToSha(templateDir, shaDir);
        AtomicBoolean pass = new AtomicBoolean(false);
        Files.readAllLines(shaFile.toPath()).forEach(x -> {
            if (x.contains("postgres@sha256")) {
                pass.set(true);
            }
        });
        assertTrue(pass.get());
    }

    @Test
    void kbldPkg() throws HelmPullException, ExternalProcessError, CouldNotDeleteTestsException, HelmTemplateException, KbldShaException, KbldPkgException {
        HelmGap testSubject = new HelmGap();
        ChartDescriptor chartDescriptor = ChartDescriptor.byShortName(repo, name, version);
        testSubject.helmPull(chartDescriptor, pullDir, chartTarTmp);
        testSubject.helmDeleteTests(chartRoot);
        testSubject.helmTemplate(chartRoot, templateDir);
        File shaFile = testSubject.kbldToSha(templateDir, shaDir);
        File archiveFile = testSubject.kbldPkg(shaFile, archiveDir, name, version);
        assertTrue(archiveFile.exists());
        System.out.println(archiveFile.toString());
    }

    @Test
    void buildAirgapByShortName() throws AirgapInstallException {
        HelmGap testSubject = new HelmGap();
        ChartDescriptor chartDescriptor = ChartDescriptor.byShortName(repo, name, version);
        AirgapInstall airgapInstall = testSubject.buildAirgap(chartDescriptor);
        logger.info("Generated airgap installer with\nRegistry: "+airgapInstall.getAirgapInstallerArchive().toString()+"\nChart: "+airgapInstall.getOriginalChart().toString());
        assertTrue(airgapInstall.getOriginalChart().exists());
        assertTrue(airgapInstall.getAirgapInstallerArchive().exists());
    }

    @Test
    void buildAirgapByChartUrl() throws AirgapInstallException {
        HelmGap testSubject = new HelmGap();
        ChartDescriptor chartDescriptor = ChartDescriptor.byChartUrl("https://kubernetes-charts.storage.googleapis.com/hackmd-0.1.0.tgz");
        AirgapInstall airgapInstall = testSubject.buildAirgap(chartDescriptor);
        logger.info("Generated airgap installer with\nRegistry: "+airgapInstall.getAirgapInstallerArchive().toString()+"\nChart: "+airgapInstall.getOriginalChart().toString());
        assertTrue(airgapInstall.getOriginalChart().exists());
        assertTrue(airgapInstall.getAirgapInstallerArchive().exists());
    }

    @Test
    void buildAirgapByRepoUrl() throws AirgapInstallException {
        HelmGap testSubject = new HelmGap();
        ChartDescriptor chartDescriptor = ChartDescriptor.byRepoUrl("https://kubernetes-charts.storage.googleapis.com", "hackmd", "0.1.0");
        AirgapInstall airgapInstall = testSubject.buildAirgap(chartDescriptor);
        logger.info("Generated airgap installer with\nRegistry: "+airgapInstall.getAirgapInstallerArchive().toString()+"\nChart: "+airgapInstall.getOriginalChart().toString());
        assertTrue(airgapInstall.getOriginalChart().exists());
        assertTrue(airgapInstall.getAirgapInstallerArchive().exists());
    }
}