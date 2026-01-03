package org.cityxplore.backend.fogofwar.validation

import com.uber.h3core.H3Core
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [H3IndexValidator::class])
annotation class ValidH3Index(
    val message: String = "Invalid H3 index",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class H3IndexValidator : ConstraintValidator<ValidH3Index, String> {
    private val h3 = H3Core.newInstance()

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle nulls
        return try {
            h3.isValidCell(value)
        } catch (_: Exception) {
            false
        }
    }
}
