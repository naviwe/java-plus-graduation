package ewm.compilation.admin;

import ewm.interaction.dto.event.CompilationDto;
import ewm.interaction.dto.event.NewCompilationDto;
import ewm.interaction.dto.event.UpdateCompilationRequest;

public interface CompilationAdminService {

    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(UpdateCompilationRequest updateCompilationRequest, Long compId);

    void deleteCompilation(Long compId);
}
