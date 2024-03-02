package searchengine.dao.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;

/**
 * PageRepository
 *
 * @author alex90bar
 */

public interface PageRepository extends JpaRepository<Page, Integer> {

    long deleteAllBySite(SiteEntity siteId);

    List<Page> findAllBySite(SiteEntity site);

    boolean existsByPath(String path);

    Page findByPath(String path);

    int countPagesBySite(SiteEntity site);

}
