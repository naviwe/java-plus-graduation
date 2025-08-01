package ewm.compilation;

import ewm.interaction.dto.event.CompilationDto;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public interface CompilationService {
    List<CompilationDto> findCompilationsByPinned(Boolean pinned, @PositiveOrZero Integer from, @Positive Integer size);

    CompilationDto findById(Long compId);
}
