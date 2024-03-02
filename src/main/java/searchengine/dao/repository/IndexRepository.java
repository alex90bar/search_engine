package searchengine.dao.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.dao.model.Index;
import searchengine.dao.model.Page;

/**
 * IndexRepository
 *
 * @author alex90bar
 */

public interface IndexRepository extends JpaRepository<Index, Integer> {

    List<Index> findByPage(Page page);

}
