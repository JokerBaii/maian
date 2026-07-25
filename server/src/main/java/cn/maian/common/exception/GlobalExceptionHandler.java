package cn.maian.common.exception;

import cn.maian.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("RESOURCE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRouteNotFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("ROUTE_NOT_FOUND", "请求接口不存在"));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(InvalidStateTransitionException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("INVALID_STATE_TRANSITION", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("VALIDATION_FAILED", "请求参数校验失败", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("VALIDATION_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(HealthAnalysisUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHealthAnalysisUnavailable(
        HealthAnalysisUnavailableException exception
    ) {
        log.warn("Health analysis unavailable", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error("HEALTH_ANALYSIS_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));
    }
}
