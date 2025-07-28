package ewm.compilation.mapper;

import ewm.event.mapper.EventMapper;
import ewm.interaction.dto.event.CompilationDto;
import ewm.interaction.dto.event.NewCompilationDto;
import ewm.interaction.dto.event.UpdateCompilationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ewm.compilation.Compilation;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto newCompilationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(UpdateCompilationRequest updateCompilationRequest);


    CompilationDto toDto(Compilation compilation);


}
