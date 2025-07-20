package ewm.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import ewm.compilation.dto.CompilationDto;

import java.util.List;

public interface CompilationService {
    List<CompilationDto> findCompilationsByPinned(Boolean pinned, @PositiveOrZero Integer from, @Positive Integer size);

    CompilationDto findById(Long compId);
}