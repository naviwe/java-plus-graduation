package ewm.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FuturePlusTwoHoursValidator implements ConstraintValidator<FuturePlusTwoHours, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean isValid(String eventDate, ConstraintValidatorContext context) {
        if (eventDate == null) {
            return true;
        }
        try {
            LocalDateTime eventDateTime = LocalDateTime.parse(eventDate, FORMATTER);
            LocalDateTime minAllowedDateTime = LocalDateTime.now().plusHours(2).minusSeconds(1);
            return eventDateTime.isAfter(minAllowedDateTime);
        } catch (Exception e) {
            return false;
        }
    }
}