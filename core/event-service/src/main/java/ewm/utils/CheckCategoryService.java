package ewm.utils;

import ewm.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ewm.category.model.Category;
import ewm.category.repository.CategoryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCategoryService {
    private final CategoryRepository categoryRepository;

    public Category checkCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(String.format("Category with id=%d was not found",
                        categoryId)));
    }
}
