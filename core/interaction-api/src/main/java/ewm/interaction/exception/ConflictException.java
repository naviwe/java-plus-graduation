package ewm.interaction.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
