package org.cityxplore.backend.shared.exception

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ApiError(
        val timestamp: Instant = Instant.now(),
        val status: Int,
        val error: String,
        val message: String?,
        val fieldErrors: Map<String, String>? = null
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        val body = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Validation failed",
            fieldErrors = errors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiError> {
        val statusCode: HttpStatusCode = ex.statusCode
        val httpStatus = HttpStatus.resolve(statusCode.value())
        val body = ApiError(
            status = statusCode.value(),
            error = httpStatus?.reasonPhrase ?: statusCode.toString(),
            message = ex.reason
        )

        return ResponseEntity.status(statusCode).body(body)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        val status = HttpStatus.BAD_REQUEST
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message
        )

        return ResponseEntity.status(status).body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiError> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message
        )

        return ResponseEntity.status(status).body(body)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiError> {
        val status = HttpStatus.BAD_REQUEST
        val msg = buildString {
            append("Invalid value '${ex.value}' for parameter '${ex.name}'")
            ex.requiredType?.let { append(", expected ${it.simpleName}") }
        }
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = msg
        )

        return ResponseEntity.status(status).body(body)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiError> {
        val status = HttpStatus.BAD_REQUEST
        val fieldErrors = ex.constraintViolations.associate { v ->
            val path = v.propertyPath.toString()
            path to (v.message ?: "invalid")
        }

        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Validation failed",
            fieldErrors = fieldErrors
        )

        return ResponseEntity.status(status).body(body)
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDenied(ex: AuthorizationDeniedException): ResponseEntity<ApiError> {
        val status = HttpStatus.FORBIDDEN
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Access Denied"
        )

        return ResponseEntity.status(status).body(body)
    }
}
