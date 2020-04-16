package uk.co.solong.helmgap;

public abstract class AirgapInstallException extends Exception {
    public AirgapInstallException(String message, Throwable cause) {
        super(message, cause);
    }

    public AirgapInstallException(String message) {
        super(message);
    }

    public AirgapInstallException(Throwable exception) {
        super(exception);
    }

    public AirgapInstallException() {
        super();
    }
}
