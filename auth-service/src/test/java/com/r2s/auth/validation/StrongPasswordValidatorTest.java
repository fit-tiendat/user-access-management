package com.r2s.auth.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Strong@123", "T0ugh#Password"})
    void shouldAcceptStrongPasswords(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Aa1!",
            "strong@123",
            "STRONG@123",
            "Strong@abc",
            "Strong123",
            "Strong 123!",
            "Password1!"
    })
    void shouldRejectWeakOrCommonPasswords(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @Test
    void shouldLetNotBlankValidatorHandleNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
