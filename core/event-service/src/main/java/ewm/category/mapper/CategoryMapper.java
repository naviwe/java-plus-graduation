package ewm.category.mapper;

import ewm.interaction.dto.event.CategoryDto;
import ewm.interaction.dto.event.NewCategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ewm.category.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toEntity(NewCategoryDto newCategoryDto);

    CategoryDto toDto(Category category);
}