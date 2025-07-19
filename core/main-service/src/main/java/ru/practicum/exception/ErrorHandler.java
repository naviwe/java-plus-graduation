package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException e) {
        return new ErrorResponse("NOT_FOUND", "The required object was not found.", e.getMessage());
    }

    @ExceptionHandler({ConflictException.class,DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException e) {
        return new ErrorResponse("CONFLICT", "Integrity constraint has been violated.", e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ValidationException e) {
        return new ErrorResponse("BAD_REQUEST", "Incorrectly made request.", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleForbidden(ForbiddenException e) {
        log.warn("Handling ForbiddenException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("FORBIDDEN",
                "For the requested operation the conditions are not met.",
                e.getMessage());
        return errorResponse;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException e) {
        List<String> errorMessages = e.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    Object rejectedValue = ((FieldError) error).getRejectedValue();
                    return "Field: " + fieldName + ". Error: " + errorMessage + ". Value: " + rejectedValue;
                })
                .toList();
        String firstErrorMessage = errorMessages.get(0);
        ErrorResponse response = new ErrorResponse(
                "BAD_REQUEST",
                "Incorrectly made request.",
                firstErrorMessage
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


}
