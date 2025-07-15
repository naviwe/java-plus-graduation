package ru.practicum.category.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import static ru.practicum.utils.LoggingUtils.logAndReturn;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    EventRepository eventRepository;

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Creating category with name: {}", newCategoryDto.getName());
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            log.warn("Category creation failed - name {} already exists", newCategoryDto.getName());
            throw new ConflictException(String.format("Name: %s already used by another category",
                    newCategoryDto.getName()));
        }
        Category category = categoryMapper.toEntity(newCategoryDto);
        return logAndReturn(
                categoryMapper.toDto(categoryRepository.save(category)),
                savedCategory -> log.info("Category created successfully: {}", savedCategory)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Fetching categories with from={} and size={}", from, size);
        Pageable page = PageRequest.of(from / size, size);
        return logAndReturn(
                categoryRepository.findAll(page)
                        .stream()
                        .map(categoryMapper::toDto)
                        .toList(),
                categories -> log.info("Fetched {} categories", categories.size())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategory(Long catId) {
        log.info("Fetching category with id={}", catId);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Category with id={} not found", catId);
                    return new NotFoundException(String.format("Category with id=%d was not found", catId));
                });
        return logAndReturn(
                categoryMapper.toDto(category),
                foundCategory -> log.info("Category found: {}", foundCategory)
        );
    }

    @Override
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Updating category with id={} to new name={}", catId, categoryDto.getName());
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Category with id={} not found for update", catId);
                    return new NotFoundException(String.format("Category with id=%d was not found", catId));
                });
        Optional<Category> optionalCategory = categoryRepository.findByName(categoryDto.getName());
        if ((optionalCategory.isPresent()) && !optionalCategory.get().getId().equals(catId)) {
            log.warn("Category update failed - name {} already exists", categoryDto.getName());
            throw new ConflictException(String.format("Name: %s already used by another category",
                    categoryDto.getName()));
        }
        category.setName(categoryDto.getName());
        return logAndReturn(
                categoryMapper.toDto(categoryRepository.save(category)),
                updatedCategory -> log.info("Category updated successfully: {}", updatedCategory)
        );
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.deleteById(catId);
    }

}
