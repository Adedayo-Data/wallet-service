package com.hng.wallet_service.exceptions;

import com.hng.wallet_service.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponseDTO> handleAccessDenied(
                        AccessDeniedException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error("Forbidden")
                                .message("Access Denied: " + ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        @ExceptionHandler(InsufficientBalanceException.class)
        public ResponseEntity<ErrorResponseDTO> handleInsufficientBalance(
                        InsufficientBalanceException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(WalletNotFoundException.class)
        public ResponseEntity<ErrorResponseDTO> handleWalletNotFound(
                        WalletNotFoundException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(InvalidAmountException.class)
        public ResponseEntity<ErrorResponseDTO> handleInvalidAmount(
                        InvalidAmountException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ErrorResponseDTO> handleUnauthorized(
                        UnauthorizedException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(ApiKeyExpiredException.class)
        public ResponseEntity<ErrorResponseDTO> handleApiKeyExpired(
                        ApiKeyExpiredException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(ApiKeyRevokedException.class)
        public ResponseEntity<ErrorResponseDTO> handleApiKeyRevoked(
                        ApiKeyRevokedException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(ApiKeyLimitExceededException.class)
        public ResponseEntity<ErrorResponseDTO> handleApiKeyLimitExceeded(
                        ApiKeyLimitExceededException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponseDTO> handleRuntimeException(
                        RuntimeException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponseDTO> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                // Log the actual exception for debugging
                ex.printStackTrace();

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                                .path(request.getRequestURI())
                                .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
