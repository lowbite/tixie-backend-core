package com.tixie.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionMappersTest {

    @Test
    void validationExceptionMapper_returnsBadRequest() {
        var mapper = new ValidationExceptionMapper();
        Response response = mapper.toResponse(new ValidationException("INVALID", "bad"));
        assertEquals(400, response.getStatus());
        var body = (ErrorResponse) response.getEntity();
        assertEquals("INVALID", body.code());
        assertEquals("bad", body.message());
    }

    @Test
    void notFoundExceptionMapper_returnsNotFound() {
        var mapper = new NotFoundExceptionMapper();
        Response response = mapper.toResponse(new NotFoundException("missing"));
        assertEquals(404, response.getStatus());
        var body = (ErrorResponse) response.getEntity();
        assertEquals("NOT_FOUND", body.code());
        assertEquals("missing", body.message());
    }

    @Test
    void constraintViolationMapper_joinsMessages() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> v = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("title");
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn("must not be blank");
        var ex = new ConstraintViolationException(Set.of(v));

        var mapper = new ConstraintViolationExceptionMapper();
        Response response = mapper.toResponse(ex);

        assertEquals(400, response.getStatus());
        var body = (ErrorResponse) response.getEntity();
        assertEquals("VALIDATION_ERROR", body.code());
        assertTrue(body.message().contains("title"));
    }

    @Test
    void errorResponseAndValidationException_accessorsWork() {
        var error = new ErrorResponse("C", "M");
        assertEquals("C", error.code());
        assertEquals("M", error.message());

        var ex = new ValidationException("X", "Y");
        assertEquals("X", ex.getCode());
        assertEquals("Y", ex.getMessage());
    }
}
