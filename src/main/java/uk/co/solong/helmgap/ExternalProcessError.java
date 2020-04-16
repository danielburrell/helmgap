package uk.co.solong.helmgap;

public class ExternalProcessError extends AirgapInstallException {

    private static final long serialVersionUID = 7718828512143293558L;


    public ExternalProcessError(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalProcessError(String message) {
        super(message);
    }

    public ExternalProcessError(Throwable cause) {
        super(cause);
    }

    public ExternalProcessError() {
        super();
    }

}
