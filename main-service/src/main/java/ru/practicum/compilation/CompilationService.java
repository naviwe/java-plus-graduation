package ru.practicum.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import ru.practicum.compilation.dto.CompilationDto;

import java.util.List;

public interface CompilationService {
    List<CompilationDto> findCompilationsByPinned(Boolean pinned, @PositiveOrZero Integer from, @Positive Integer size);

    CompilationDto findById(Long compId);
}
