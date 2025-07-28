package ewm.compilation.admin;


import ewm.interaction.dto.event.CompilationDto;
import ewm.interaction.dto.event.NewCompilationDto;
import ewm.interaction.dto.event.UpdateCompilationRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompilationAdminController {


    CompilationAdminService compilationAdminService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {

        return compilationAdminService.createCompilation(newCompilationDto);
    }


    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(@RequestBody @Valid UpdateCompilationRequest updateCompilationRequest,
                                            @PathVariable Long compId) {
        return compilationAdminService.updateCompilation(updateCompilationRequest,compId);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Long compId) {
        compilationAdminService.deleteCompilation(compId);
    }
}
