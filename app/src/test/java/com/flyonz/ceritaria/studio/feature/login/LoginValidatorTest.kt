package com.flyonz.ceritaria.studio.feature.login

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginValidatorTest {
    @Test
    fun validCredentialsPassValidation() {
        assertEquals(
            LoginValidationResult.Valid,
            LoginValidator.validate("admin@ceritaria.site", "secret123"),
        )
    }

    @Test
    fun malformedEmailIsRejected() {
        assertEquals(
            LoginValidationResult.InvalidEmail,
            LoginValidator.validate("admin", "secret123"),
        )
    }

    @Test
    fun shortPasswordIsRejected() {
        assertEquals(
            LoginValidationResult.InvalidPassword,
            LoginValidator.validate("admin@ceritaria.site", "123"),
        )
    }
}
