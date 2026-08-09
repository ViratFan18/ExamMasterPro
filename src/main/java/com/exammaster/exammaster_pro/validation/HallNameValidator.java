package com.exammaster.exammaster_pro.validation;

public final class HallNameValidator {
    public static final String REGEX = "^[A-Za-z0-9._-]{1,50}$";

    private HallNameValidator() {}

    public static boolean isValid(String value) {
        return value != null && value.matches(REGEX);
    }
}
