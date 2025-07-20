package ewm.compilation;


import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import ewm.compilation.dto.CompilationDto;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationPublicController {
    final String compilationIdPath = "/{compId}";
    final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> findCompilationsByPinned(@RequestParam(required = false) Boolean pinned,
                                                        @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                                        @Positive @RequestParam(defaultValue = "10") Integer size) {
        return compilationService.findCompilationsByPinned(pinned, from, size);
    }

    @GetMapping(compilationIdPath)
    public CompilationDto findById(@PathVariable Long compId) {
        return compilationService.findById(compId);
    }
}
