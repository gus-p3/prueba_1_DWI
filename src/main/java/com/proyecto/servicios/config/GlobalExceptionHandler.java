package com.proyecto.servicios.config;

import com.proyecto.servicios.model.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException capturada: {}", ex.getMessage(), ex);

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("INTERNAL_ERROR");
        detail.setMessage(ex.getMessage());

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Error de validación: {}", ex.getMessage());

        List<ErrorResponse.ErrorDetailItem> detailItems = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            ErrorResponse.ErrorDetailItem item = new ErrorResponse.ErrorDetailItem();
            item.setCode("INVALID_FIELD");
            item.setTarget(fieldError.getField());
            item.setMessage(fieldError.getDefaultMessage());
            detailItems.add(item);
        });

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("BAD_REQUEST");
        detail.setMessage("Error de validación en la solicitud");
        detail.setDetails(detailItems);

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Excepción no controlada capturada: {}", ex.getMessage(), ex);

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("SERVER_ERROR");
        detail.setMessage(ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor");

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
