package searchengine.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.dao.model.SiteEntity;

/**
 * SiteRepository
 *
 * @author alex90bar
 */

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    SiteEntity findByUrl(String url);

}
