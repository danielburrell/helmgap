package uk.co.solong.helmgap.kbld;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import uk.co.solong.helmgap.ExternalProcessError;
import uk.co.solong.helmgap.KbldLockFileGenerationException;
import uk.co.solong.helmgap.KbldPkgException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

public class Kbld {
    private static final Logger logger = LoggerFactory.getLogger(Kbld.class);
    private final String kbld;

    public Kbld(String kbld) {
        this.kbld = kbld;
    }

    public File pkg(File lockFile, Path archiveDir, String name, String version) throws KbldPkgException, ExternalProcessError {
        logger.info("Generating Package Archive");
        ProcessBuilder builder = new ProcessBuilder();
        File archiveFile = Paths.get(archiveDir.toString(), name + "-airgap-" + version + ".tgz").toFile();

        builder.command(kbld, "pkg", "-f", lockFile.toString(), "-o", archiveFile.toString());
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

    public File generateLockFile(Path templateDirectory, Path shaDir, String chartName, String chartVersion) throws ExternalProcessError, KbldLockFileGenerationException {
        String output;
        try {
            File lockFile = Paths.get(shaDir.toString(), chartName+"-"+chartVersion+"-lock.yml").toFile();
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
}
