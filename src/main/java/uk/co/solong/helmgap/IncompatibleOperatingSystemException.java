package uk.co.solong.helmgap;

public class IncompatibleOperatingSystemException extends AirgapInstallException {
    private final String osName;

    public IncompatibleOperatingSystemException(String message, Throwable cause, String osName) {
        super(message, cause);
        this.osName = osName;
    }

    public IncompatibleOperatingSystemException(String message, String osName) {
        super(message);
        this.osName = osName;
    }

    public IncompatibleOperatingSystemException(Throwable cause, String osName) {
        super(cause);
        this.osName = osName;
    }

    public IncompatibleOperatingSystemException(String osName) {
        this.osName = osName;
    }

    public String getOsName() {
        return osName;
    }

    @Override
    public String getMessage() {
        return "Airgap library is only supported on UNIX-like operating systems. Detected "+osName;
    }
}
