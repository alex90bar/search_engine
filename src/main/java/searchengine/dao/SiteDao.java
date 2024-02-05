package searchengine.dao;

import searchengine.dao.model.SiteEntity;

/**
 * SiteDao
 *
 * @author alex90bar
 */

public interface SiteDao {

    SiteEntity getByUrl(String url);

    void deleteById(Integer id);

    SiteEntity update(SiteEntity siteEntity);

    void setStatusFailed(SiteEntity siteEntity);

}


