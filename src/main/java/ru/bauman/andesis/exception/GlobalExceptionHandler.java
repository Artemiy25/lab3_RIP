package ru.bauman.andesis.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import ru.bauman.andesis.dto.ErrorResponse;

import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidParametersException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameters(
            InvalidParametersException ex,
            ServerWebExchange exchange) {

        log.warn("Invalid parameters provided: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .error("INVALID_PARAMETERS")
                .message(ex.getMessage())
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(
            TimeoutException ex,
            ServerWebExchange exchange) {

        log.error("Request timeout: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .error("REQUEST_TIMEOUT")
                .message("Request processing exceeded the timeout limit")
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.GATEWAY_TIMEOUT.value())
                .build();

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            ServerWebExchange exchange) {

        log.warn("Resource not found: {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getPath());

        ErrorResponse response = ErrorResponse.builder()
                .error("NOT_FOUND")
                .message("The requested resource was not found")
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.NOT_FOUND.value())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            ServerWebExchange exchange) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse response = ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
