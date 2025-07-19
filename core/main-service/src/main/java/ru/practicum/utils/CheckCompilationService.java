package ru.practicum.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.compilation.Compilation;
import ru.practicum.compilation.CompilationRepository;
import ru.practicum.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCompilationService {
    private final CompilationRepository compilationRepository;

    public Compilation checkCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d was not found",
                        compId)));
    }
}
