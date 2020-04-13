package uk.co.solong.helmgap;

public class KbldPkgException extends AirgapInstallException {
    private final String errorOutput;

    public KbldPkgException(String errorOutput) {
        this.errorOutput = errorOutput;
    }

    public String getErrorOutput() {
        return errorOutput;
    }
}
