package ewm.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FuturePlusTwoHoursValidator.class)
public @interface FuturePlusTwoHours {
    String message() default "должно содержать дату, которая еще не наступила.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
