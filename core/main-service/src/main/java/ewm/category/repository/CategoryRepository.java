package ewm.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ewm.category.model.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    Boolean existsByName(String name);
}
