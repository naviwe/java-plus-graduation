package ru.practicum.compilation;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.utils.CheckCompilationService;


import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompilationServiceImpl implements CompilationService {

    CompilationMapper compilationMapper;
    CompilationRepository compilationRepository;
    CheckCompilationService checkCompilationService;

    @Override
    public List<CompilationDto> findCompilationsByPinned(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable);
        } else {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        }
        return logAndReturn(compilations.stream()
                        .map(compilationMapper::toDto)
                        .collect(Collectors.toList()),
                comp -> log.info("Found {} compilations", comp.size()));
    }

    @Override
    public CompilationDto findById(Long compId) {
        return logAndReturn(compilationMapper.toDto(checkCompilationService.checkCompilation(compId)),
                comp -> log.info("Found compilation with id={}",
                        comp.getId())
        );
    }
}
