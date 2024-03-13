package searchengine.dao;

import java.util.List;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.SiteEntity;

/**
 * LemmaDao
 *
 * @author alex90bar
 */

public interface LemmaDao {

    Lemma findByLemmaAndSite(String lemma, SiteEntity siteEntity);

    Lemma update(Lemma lemma);

    List<Lemma> updateList(List<Lemma> lemmaList);

    void deleteLemmaList(List<Lemma> lemmaList);

    long getTotalCount();

    int countLemmasBySite(SiteEntity site);

}
