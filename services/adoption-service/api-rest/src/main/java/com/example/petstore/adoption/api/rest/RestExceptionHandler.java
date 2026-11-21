package com.example.petstore.adoption.api.rest;

import com.example.petstore.adoption.domain.exception.PetNotAvailableException;
import com.example.petstore.adoption.domain.exception.PetNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * El único sitio donde las excepciones de dominio se convierten en HTTP. Los errores de validación
 * los resuelve {@link ResponseEntityExceptionHandler} con ProblemDetail.
 */
@RestControllerAdvice
class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PetNotFoundException.class)
    ProblemDetail petNotFound(PetNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PetNotAvailableException.class)
    ProblemDetail petNotAvailable(PetNotAvailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
