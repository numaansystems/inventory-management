package com.numaansystems.inventory.common;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every error as an RFC 9457 {@code application/problem+json} body. All problems carry a
 * machine-readable {@code code}; validation failures add an {@code errors} list of field messages.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final URI TYPE_NOT_FOUND = URI.create("urn:problem:not-found");
    static final URI TYPE_VALIDATION = URI.create("urn:problem:validation");
    static final URI TYPE_BUSINESS_RULE = URI.create("urn:problem:business-rule");
    static final URI TYPE_CONCURRENT_UPDATE = URI.create("urn:problem:concurrent-update");

    public record FieldMessage(String field, String message) {}

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, TYPE_NOT_FOUND, "Resource not found", ex.getMessage(), "NOT_FOUND");
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.CONFLICT, TYPE_BUSINESS_RULE, "Business rule violated", ex.getMessage(), ex.code());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return validationProblem(List.of(new FieldMessage(ex.field(), ex.getMessage())));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleUnknownSortProperty(PropertyReferenceException ex) {
        return validationProblem(List.of(new FieldMessage("sort", "unknown property '" + ex.getPropertyName() + "'")));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, TYPE_CONCURRENT_UPDATE, "Concurrent update",
                "The resource was modified by another request; reload and retry.", "CONCURRENT_UPDATE");
    }

    /** Last line of defence for races the services' own checks cannot see, e.g. two concurrent creates of one SKU. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, TYPE_BUSINESS_RULE, "Data integrity violation",
                "The request conflicts with existing data.", "DATA_INTEGRITY_VIOLATION");
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldMessage> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldMessage(error.getField(), messageOf(error)))
                .sorted(Comparator.comparing(FieldMessage::field).thenComparing(FieldMessage::message))
                .toList();
        ProblemDetail body = validationProblem(errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldMessage> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldMessage(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        ProblemDetail body = validationProblem(errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // Give framework-generated problems (malformed JSON, unsupported method, ...) a code as well.
        if (body instanceof ProblemDetail detail
                && (detail.getProperties() == null || !detail.getProperties().containsKey("code"))) {
            detail.setProperty("code", HttpStatus.resolve(statusCode.value()) instanceof HttpStatus known
                    ? known.name()
                    : "ERROR");
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ProblemDetail validationProblem(List<FieldMessage> errors) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, TYPE_VALIDATION, "Validation failed",
                "The request contains invalid fields.", "VALIDATION_FAILED");
        detail.setProperties(Map.of("code", "VALIDATION_FAILED", "errors", errors));
        return detail;
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid";
    }
}
