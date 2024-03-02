package searchengine.dao;

import java.util.List;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;

/**
 * PageDao
 *
 * @author alex90bar
 */

public interface PageDao {

    long deleteAllBySite(SiteEntity site);

    void deleteAll();

    List<Page> findAllBySite(SiteEntity site);

    boolean existsByPath(String path);

    Page update(Page page);

    Page findByPath(String path);

    void delete(Page page);

    long getTotalCount();

}


