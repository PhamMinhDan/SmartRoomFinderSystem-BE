package com.smartroomfinder.smartroomfinder.exceptions;

import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) {

        ApiResponse<?> response = ApiResponse.error(
                ex.getMessage(),
                "RESOURCE_NOT_FOUND"
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        ApiResponse<?> response = ApiResponse.error(
                message,
                "VALIDATION_ERROR"
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {

        ApiResponse<?> response = ApiResponse.error(
                ex.getMessage(),
                "BAD_REQUEST"
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {

        ApiResponse<?> response = ApiResponse.error(
                "Hệ thống gặp lỗi, vui lòng thử lại sau",
                ex.getClass().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
