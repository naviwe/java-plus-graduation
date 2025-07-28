package ewm.interaction.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtils {
    public static <T> T logAndReturn(T result, Consumer<T> logAction) {
        logAction.accept(result);
        return result;
    }
}

