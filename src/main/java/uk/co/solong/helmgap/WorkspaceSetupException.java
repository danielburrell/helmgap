package uk.co.solong.helmgap;

import java.io.IOException;

public class WorkspaceSetupException extends AirgapInstallException {

    private static final long serialVersionUID = 7718828512143293558L;


    public WorkspaceSetupException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkspaceSetupException(String message) {
        super(message);
    }

    public WorkspaceSetupException(Throwable cause) {
        super(cause);
    }

    public WorkspaceSetupException() {
        super();
    }

}
