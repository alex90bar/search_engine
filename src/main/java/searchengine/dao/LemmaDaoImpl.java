package searchengine.dao;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.LemmaRepository;

/**
 * LemmaDaoImpl
 *
 * @author alex90bar
 */

@Repository
@RequiredArgsConstructor
public class LemmaDaoImpl implements LemmaDao {

    private final LemmaRepository lemmaRepository;

    @Override
    @Transactional(readOnly = true)
    public Lemma findByLemmaAndSite(String lemma, SiteEntity siteEntity) {
        return lemmaRepository.findByLemmaAndSite(lemma, siteEntity);
    }

    @Override
    @Transactional
    public Lemma update(Lemma lemma) {
        return lemmaRepository.save(lemma);
    }

    @Override
    @Transactional
    public void deleteLemmaList(List<Lemma> lemmaList) {
        lemmaRepository.deleteAll(lemmaList);
    }

    @Override
    @Transactional
    public void deleteAll() {
        lemmaRepository.deleteAll();
    }

    @Override
    @Transactional
    public List<Lemma> updateList(List<Lemma> lemmaList) {
        return lemmaRepository.saveAll(lemmaList);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return lemmaRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public int countLemmasBySite(SiteEntity site) {
        return lemmaRepository.countLemmaBySite(site);
    }
}
