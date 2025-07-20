package ewm.event.dto;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.validation.FuturePlusTwoHours;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {

    @NotBlank
    @Size(min = 20, max = 2000)
    String annotation;

    @NotNull
    Long category;

    @NotBlank
    @Size(min = 20, max = 7000)
    String description;

    @NotBlank
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @FuturePlusTwoHours
    String eventDate;

    @NotNull
    LocationDto location;

    Boolean paid = false;

    @PositiveOrZero
    Integer participantLimit = 0;

    Boolean requestModeration = true;

    @NotBlank
    @Size(min = 3, max = 120)
    String title;
}
