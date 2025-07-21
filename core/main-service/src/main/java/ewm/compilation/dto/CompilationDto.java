package ewm.compilation.dto;


import ewm.event.dto.EventShortDto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {

    Long id;

    Boolean pinned;

    String title;

    Set<EventShortDto> events;
}
