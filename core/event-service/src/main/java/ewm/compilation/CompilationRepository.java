package ewm.compilation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompilationRepository extends JpaRepository<Compilation,Long> {
    Page<Compilation> findByPinned(Boolean pinned, Pageable pageable);
}
