package com.exammaster.exammaster_pro.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HallNameValidatorTest {

    @Test
    void allowsCommonHallNameFormatsWithoutSpaces() {
        assertTrue(HallNameValidator.isValid("Hall-A"));
        assertTrue(HallNameValidator.isValid("hall-a"));
        assertTrue(HallNameValidator.isValid("Hall_1"));
        assertTrue(HallNameValidator.isValid("A1"));
    }

    @Test
    void rejectsSpacesAndUnsupportedCharacters() {
        assertFalse(HallNameValidator.isValid("Hall A"));
        assertFalse(HallNameValidator.isValid("Hall/A"));
        assertFalse(HallNameValidator.isValid(""));
    }
}
