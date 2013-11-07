package ISIS.database;


//TODO: we need to catch all exceptions and runtime exceptions at the window level, and exit the view on catching (since we didn't
// explicitly catch them, we don't know if they're recoverable)
public class RecordSaveException extends RuntimeException {
    public RecordSaveException() {
        super();
    }

    public RecordSaveException(String message) {
        super(message);
    }
}
