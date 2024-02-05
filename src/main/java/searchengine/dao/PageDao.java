package searchengine.dao;

import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;

/**
 * PageDao
 *
 * @author alex90bar
 */

public interface PageDao {

    long deleteAllBySite(SiteEntity site);

    boolean existsByPath(String path);

    Page update(Page page);

}


