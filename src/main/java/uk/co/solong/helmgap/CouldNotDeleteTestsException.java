package uk.co.solong.helmgap;

public class CouldNotDeleteTestsException extends AirgapInstallException {

    private static final long serialVersionUID = 7718828512143293558L;


    public CouldNotDeleteTestsException(String message, Throwable cause) {
        super(message, cause);
    }

    public CouldNotDeleteTestsException(String message) {
        super(message);
    }

    public CouldNotDeleteTestsException(Throwable cause) {
        super(cause);
    }

    public CouldNotDeleteTestsException() {
        super();
    }

}
