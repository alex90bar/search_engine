package searchengine.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.SiteEntity;

/**
 * LemmaRepository
 *
 * @author alex90bar
 */

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    Lemma findByLemmaAndSite(String lemma, SiteEntity siteEntity);

}
