package ewm.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ewm.category.model.Category;
import ewm.category.dto.CategoryDto;
import ewm.category.dto.NewCategoryDto;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toEntity(NewCategoryDto newCategoryDto);

    CategoryDto toDto(Category category);
}