package uk.co.solong.helmgap;

public class MalformedChartException extends AirgapInstallException {

    private static final long serialVersionUID = 7718828512143293558L;


    public MalformedChartException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedChartException(String message) {
        super(message);
    }

    public MalformedChartException(Throwable cause) {
        super(cause);
    }

    public MalformedChartException() {
        super();
    }

}
