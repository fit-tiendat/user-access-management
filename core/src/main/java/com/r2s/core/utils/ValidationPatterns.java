package com.r2s.core.utils;

public final class ValidationPatterns {

    public static final String USERNAME = "^[A-Za-z0-9._-]+$";
    public static final String USERNAME_MESSAGE =
            "Username may only contain letters, numbers, dots, underscores, and hyphens";

    public static final String DISPLAY_NAME =
            "^(?:[\\p{L}\\p{M}]|[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*[\\p{L}\\p{M}])$";
    public static final String DISPLAY_NAME_MESSAGE =
            "Full name contains unsupported characters";

    private ValidationPatterns() {
    }
}
