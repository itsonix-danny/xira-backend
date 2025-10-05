package eu.itsonix.genai.xira.web;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import eu.itsonix.genai.xira.web.model.Problem;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Problem> handleUsernameNotFound(final UsernameNotFoundException ex) {
        return problemResponse(UNAUTHORIZED, ex);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Problem> handleAuthenticationException(final Exception ex) {
        return problemResponse(UNAUTHORIZED, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Problem> handleAccessDenied(final AccessDeniedException ex) {
        return problemResponse(FORBIDDEN, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidationException(final MethodArgumentNotValidException ex) {
        return problemResponse(BAD_REQUEST, ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Problem> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        return problemResponse(BAD_REQUEST, ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleMessageNotReadable(final HttpMessageNotReadableException ex) {
        return problemResponse(BAD_REQUEST, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(final ConstraintViolationException ex) {
        return problemResponse(BAD_REQUEST, ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Problem> handleMissingParameter(final MissingServletRequestParameterException ex) {
        return problemResponse(BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Problem> handleIllegalArgument(final IllegalArgumentException ex) {
        return problemResponse(BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Problem> handleIllegalState(final IllegalStateException ex) {
        return problemResponse(CONFLICT, ex);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Problem> handleNotFoundArgument(final EntityNotFoundException ex) {
        return problemResponse(NOT_FOUND, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleGeneral(final Exception ex) {
        return problemResponse(INTERNAL_SERVER_ERROR, ex);
    }

    private ResponseEntity<Problem> problemResponse(final HttpStatus status, final Exception e) {
        final Problem problem = new Problem();
        problem.setDetail(status.getReasonPhrase());
        problem.setStatus(status.value());
        problem.setDetail(e.getMessage());
        return ResponseEntity.status(status).contentType(APPLICATION_PROBLEM_JSON).body(problem);
    }
}