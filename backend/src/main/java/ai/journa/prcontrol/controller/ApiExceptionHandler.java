package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.ErrorResponse;
import ai.journa.prcontrol.exception.EmailAlreadyRegisteredException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<ErrorResponse> handleEmailConflict(EmailAlreadyRegisteredException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(ex.getMessage(), "EMAIL_ALREADY_REGISTERED"));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), "BAD_REQUEST"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(new ErrorResponse(ex.getReason(), ex.getStatusCode().toString()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getAllErrors().stream()
        .findFirst()
        .map(err -> err.getDefaultMessage())
        .orElse("Validation error");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(message, "VALIDATION_ERROR"));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ex.getMessage(), "VALIDATION_ERROR"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("Unexpected server error", "INTERNAL_ERROR"));
  }
}
