package uk.co.solong.helmgap;

public class FailedToDetermineChartRootException extends AirgapInstallException {

    private static final long serialVersionUID = 7718828512143293558L;


    public FailedToDetermineChartRootException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToDetermineChartRootException(String message) {
        super(message);
    }

    public FailedToDetermineChartRootException(Throwable cause) {
        super(cause);
    }

    public FailedToDetermineChartRootException() {
        super();
    }

}
