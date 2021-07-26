package uk.co.solong.helmgap;

import org.zeroturnaround.exec.InvalidExitValueException;

public class KbldLockFileGenerationException extends AirgapInstallException {
    public KbldLockFileGenerationException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
