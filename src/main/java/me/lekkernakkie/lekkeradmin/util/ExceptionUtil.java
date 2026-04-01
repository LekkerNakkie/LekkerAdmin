package me.lekkernakkie.lekkeradmin.util;

public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static String getMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        return message;
    }
}