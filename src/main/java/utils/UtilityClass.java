package utils;

public final class UtilityClass {

    private UtilityClass() {
        getException();
    }

    public static void getException() {
        throw new IllegalStateException("Utility class");
    }

}
