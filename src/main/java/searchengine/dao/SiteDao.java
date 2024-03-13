package searchengine.dao;

import java.util.List;
import searchengine.dao.model.SiteEntity;

/**
 * SiteDao
 *
 * @author alex90bar
 */

public interface SiteDao {

    SiteEntity getByUrl(String url);

    List<SiteEntity> getAll();

    void deleteById(Integer id);

    SiteEntity update(SiteEntity siteEntity);

    void setStatusFailed(SiteEntity siteEntity);

    long getTotalCount();

}


