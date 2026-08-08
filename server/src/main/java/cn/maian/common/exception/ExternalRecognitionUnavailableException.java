package cn.maian.common.exception;

public class ExternalRecognitionUnavailableException extends RuntimeException {
    public ExternalRecognitionUnavailableException(String message) {
        super(message);
    }

    public ExternalRecognitionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
