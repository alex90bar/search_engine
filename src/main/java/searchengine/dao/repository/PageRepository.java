package searchengine.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;

/**
 * PageRepository
 *
 * @author alex90bar
 */

public interface PageRepository extends JpaRepository<Page, Integer> {

    long deleteAllBySiteId(SiteEntity siteId);

    boolean existsByPath(String path);

}
