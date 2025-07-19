package ru.practicum.compilation.admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.Compilation;
import ru.practicum.compilation.CompilationRepository;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.utils.CheckCompilationService;
import ru.practicum.utils.CheckEventService;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompilationAdminServiceImpl implements CompilationAdminService {

    CompilationMapper compilationMapper;
    CheckCompilationService checkCompilationService;
    CheckEventService checkEventService;
    CompilationRepository compilationRepository;


    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);
        compilation.setEvents(newCompilationDto.getEvents().stream()
                .map(checkEventService::checkEvent).collect(Collectors.toSet()));
        return compilationMapper.toDto(compilationRepository.save(compilation));
    }


    @Override
    @Transactional
    public CompilationDto updateCompilation(UpdateCompilationRequest updateCompilationRequest,Long compId) {
        Compilation compilation = checkCompilationService.checkCompilation(compId);

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }
        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }
        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            compilation.setEvents(updateCompilationRequest.getEvents().stream()
                    .map(checkEventService::checkEvent).collect(Collectors.toSet()));
        }
        return compilationMapper.toDto(compilationRepository.save(compilation));
    }


    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = checkCompilationService.checkCompilation(compId);
        compilationRepository.delete(compilation);
    }
}
