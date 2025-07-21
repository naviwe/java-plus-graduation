package ewm.category.controller;

import ewm.category.dto.CategoryDto;
import ewm.category.dto.NewCategoryDto;
import ewm.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/admin/categories")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryAdminController {
    CategoryService categoryService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto saveCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        return categoryService.createCategory(newCategoryDto);
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCategory(@Valid @RequestBody CategoryDto categoryDto,
                                      @PathVariable Long catId) {
        return categoryService.updateCategory(catId, categoryDto);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{catId}")
    public void deleteCategory(@PathVariable Long catId) {
        categoryService.deleteCategory(catId);
    }
}
