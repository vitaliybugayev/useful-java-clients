package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.testng.TestException;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

@Slf4j
public final class CommonUtils {

    private static final Random RANDOM = new Random();

    private CommonUtils() {
        UtilityClass.getException();
    }

    public static <T> List<T> filterEntities(List<T> entities, Predicate<T> filter) {
        return entities.stream()
                .filter(filter)
                .toList();
    }

    public static void sleep(long timeInMillis) {
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep was interrupted", e);
        }
    }

    public static <T> T randomChoice(List<T> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        return values.get(RANDOM.nextInt(values.size()));
    }

    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(RANDOM.nextInt(characters.length())));
        }
        return result.toString();
    }

    public static <T> T unmarshall(String value, TypeReference<T> typeReference) {
        try {
            return new ObjectMapper().readValue(value, typeReference);
        } catch (JsonProcessingException e) {
            throw new TestException(e);
        }
    }

    public static int generateRandomNumber(int min, int max) {
        return RANDOM.nextInt(max - min + 1) + min;
    }
}