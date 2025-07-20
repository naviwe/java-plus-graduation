package ewm.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ewm.compilation.Compilation;
import ewm.compilation.dto.CompilationDto;
import ewm.compilation.dto.NewCompilationDto;
import ewm.compilation.dto.UpdateCompilationRequest;
import ewm.event.mapper.EventMapper;

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
