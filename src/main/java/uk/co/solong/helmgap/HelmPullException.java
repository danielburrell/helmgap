package uk.co.solong.helmgap;

public class HelmPullException extends AirgapInstallException {

    private static final long serialVersionUID = 7718828512143293558L;


    public HelmPullException(String message, Throwable cause) {
        super(message, cause);
    }

    public HelmPullException(String message) {
        super(message);
    }

    public HelmPullException(Throwable cause) {
        super(cause);
    }

    public HelmPullException() {
        super();
    }

}
