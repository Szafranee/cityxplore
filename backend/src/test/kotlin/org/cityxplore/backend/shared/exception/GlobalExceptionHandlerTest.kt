package org.cityxplore.backend.shared.exception

import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import jakarta.validation.metadata.ConstraintDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

/**
 * Unit tests for GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleValidation should return 400 with field errors`() {
        // Given
        val bindingResult = mockk<BindingResult> {
            every { fieldErrors } returns listOf(
                FieldError("user", "email", "must be a valid email"),
                FieldError("user", "name", "must not be blank")
            )
        }
        val exception = MethodArgumentNotValidException(mockk<MethodParameter>(), bindingResult)

        // When
        val response = handler.handleValidation(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Validation failed", response.body?.message)
        assertEquals(2, response.body?.fieldErrors?.size)
        assertEquals("must be a valid email", response.body?.fieldErrors?.get("email"))
        assertEquals("must not be blank", response.body?.fieldErrors?.get("name"))
    }

    @Test
    fun `handleValidation should handle null default message`() {
        // Given
        val bindingResult = mockk<BindingResult> {
            every { fieldErrors } returns listOf(
                FieldError("user", "email", null, false, null, null, null)
            )
        }
        val exception = MethodArgumentNotValidException(mockk<MethodParameter>(), bindingResult)

        // When
        val response = handler.handleValidation(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("invalid", response.body?.fieldErrors?.get("email"))
    }

    @Test
    fun `handleResponseStatus should return appropriate status code`() {
        // Given
        val exception = ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")

        // When
        val response = handler.handleResponseStatus(exception)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
        assertEquals("Not Found", response.body?.error)
        assertEquals("Resource not found", response.body?.message)
    }

    @Test
    fun `handleResponseStatus should handle different status codes`() {
        // Given
        val exception = ResponseStatusException(HttpStatus.CONFLICT, "Conflict occurred")

        // When
        val response = handler.handleResponseStatus(exception)

        // Then
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(409, response.body?.status)
        assertEquals("Conflict", response.body?.error)
        assertEquals("Conflict occurred", response.body?.message)
    }

    @Test
    fun `handleIllegalArgument should return 400`() {
        // Given
        val exception = IllegalArgumentException("Invalid argument provided")

        // When
        val response = handler.handleIllegalArgument(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Invalid argument provided", response.body?.message)
    }

    @Test
    fun `handleGeneric should return 500`() {
        // Given
        val exception = RuntimeException("Unexpected error")

        // When
        val response = handler.handleGeneric(exception)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(500, response.body?.status)
        assertEquals("Internal Server Error", response.body?.error)
        assertEquals("Unexpected error", response.body?.message)
    }

    @Test
    fun `handleTypeMismatch should return 400 with appropriate message`() {
        // Given
        val exception = mockk<MethodArgumentTypeMismatchException> {
            every { value } returns "abc"
            every { name } returns "userId"
            every { requiredType } returns java.util.UUID::class.java
        }

        // When
        val response = handler.handleTypeMismatch(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Invalid value 'abc' for parameter 'userId', expected UUID", response.body?.message)
    }

    @Test
    fun `handleTypeMismatch should handle null required type`() {
        // Given
        val exception = mockk<MethodArgumentTypeMismatchException> {
            every { value } returns "invalid"
            every { name } returns "param"
            every { requiredType } returns null
        }

        // When
        val response = handler.handleTypeMismatch(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid value 'invalid' for parameter 'param'", response.body?.message)
    }

    @Test
    fun `handleConstraintViolation should return 400 with field errors`() {
        // Given
        val path1 = object : Path {
            override fun iterator(): MutableIterator<Path.Node> = mutableListOf<Path.Node>().iterator()
            override fun toString() = "email"
        }

        val violation1 = object : ConstraintViolation<Any> {
            override fun getMessage() = "must be a valid email"
            override fun getMessageTemplate() = ""
            override fun getRootBean() = Any()
            override fun getRootBeanClass() = Any::class.java
            override fun getLeafBean() = Any()
            override fun getExecutableParameters(): Array<Any>? = null
            override fun getExecutableReturnValue(): Any? = null
            override fun getPropertyPath() = path1
            override fun getInvalidValue(): Any? = null
            override fun getConstraintDescriptor(): ConstraintDescriptor<*> = mockk()

            @Suppress("UNCHECKED_CAST")
            override fun <U : Any> unwrap(type: Class<U>?): U = this as U
        }

        val path2 = object : Path {
            override fun iterator(): MutableIterator<Path.Node> = mutableListOf<Path.Node>().iterator()
            override fun toString() = "age"
        }

        val violation2 = object : ConstraintViolation<Any> {
            override fun getMessage() = "must be positive"
            override fun getMessageTemplate() = ""
            override fun getRootBean() = Any()
            override fun getRootBeanClass() = Any::class.java
            override fun getLeafBean() = Any()
            override fun getExecutableParameters(): Array<Any>? = null
            override fun getExecutableReturnValue(): Any? = null
            override fun getPropertyPath() = path2
            override fun getInvalidValue(): Any? = null
            override fun getConstraintDescriptor(): ConstraintDescriptor<*> = mockk()

            @Suppress("UNCHECKED_CAST")
            override fun <U : Any> unwrap(type: Class<U>?): U = this as U
        }

        val exception = ConstraintViolationException(setOf(violation1, violation2))

        // When
        val response = handler.handleConstraintViolation(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Validation failed", response.body?.message)
        assertEquals(2, response.body?.fieldErrors?.size)
        assertEquals("must be a valid email", response.body?.fieldErrors?.get("email"))
        assertEquals("must be positive", response.body?.fieldErrors?.get("age"))
    }

    @Test
    fun `handleConstraintViolation should handle null violation message`() {
        // Given
        val path = object : Path {
            override fun iterator(): MutableIterator<Path.Node> = mutableListOf<Path.Node>().iterator()
            override fun toString() = "field"
        }

        val violation = object : ConstraintViolation<Any> {
            override fun getMessage(): String? = null
            override fun getMessageTemplate() = ""
            override fun getRootBean() = Any()
            override fun getRootBeanClass() = Any::class.java
            override fun getLeafBean() = Any()
            override fun getExecutableParameters(): Array<Any>? = null
            override fun getExecutableReturnValue(): Any? = null
            override fun getPropertyPath() = path
            override fun getInvalidValue(): Any? = null
            override fun getConstraintDescriptor(): ConstraintDescriptor<*> = mockk()

            @Suppress("UNCHECKED_CAST")
            override fun <U : Any> unwrap(type: Class<U>?): U = this as U
        }

        val exception = ConstraintViolationException(setOf(violation))

        // When
        val response = handler.handleConstraintViolation(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("invalid", response.body?.fieldErrors?.get("field"))
    }

    @Test
    fun `handleAuthorizationDenied should return 403`() {
        // Given
        val authResult = mockk<AuthorizationResult>()
        every { authResult.isGranted } returns false
        val exception = AuthorizationDeniedException("Access denied", authResult)

        // When
        val response = handler.handleAuthorizationDenied(exception)

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals(403, response.body?.status)
        assertEquals("Forbidden", response.body?.error)
        assertEquals("Access Denied", response.body?.message)
    }

    @Test
    fun `ApiError should have timestamp`() {
        // Given
        val bindingResult = mockk<BindingResult> {
            every { fieldErrors } returns emptyList()
        }
        val exception = MethodArgumentNotValidException(mockk<MethodParameter>(), bindingResult)

        // When
        val response = handler.handleValidation(exception)

        // Then
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `handleValidation should return empty field errors when no errors`() {
        // Given
        val bindingResult = mockk<BindingResult> {
            every { fieldErrors } returns emptyList()
        }
        val exception = MethodArgumentNotValidException(mockk<MethodParameter>(), bindingResult)

        // When
        val response = handler.handleValidation(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(0, response.body?.fieldErrors?.size)
    }
}
