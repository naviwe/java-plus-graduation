package ewm.compilation.dto;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ewm.event.dto.EventShortDto;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {

    Long id;

    Boolean pinned;

    String title;

    Set<EventShortDto> events;
}
