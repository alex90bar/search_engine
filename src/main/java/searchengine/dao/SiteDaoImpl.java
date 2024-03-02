package searchengine.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.SiteRepository;

/**
 * SiteDaoImpl
 *
 * @author alex90bar
 */

@Repository
@RequiredArgsConstructor
public class SiteDaoImpl implements SiteDao {

    private final SiteRepository siteRepository;

    private static final String INDEXING_IS_STOPPED_BY_USER_MESS = "Индексация остановлена пользователем";

    @Override
    @Transactional(readOnly = true)
    public SiteEntity getByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        siteRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteAll() {
        siteRepository.deleteAll();
    }

    @Override
    @Transactional
    public SiteEntity update(SiteEntity siteEntity) {
        return siteRepository.save(siteEntity);
    }

    @Override
    @Transactional
    public void setStatusFailed(SiteEntity siteEntity) {
        siteEntity.setStatus(IndexingStatus.FAILED);
        siteEntity.setLastError(INDEXING_IS_STOPPED_BY_USER_MESS);
        update(siteEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return siteRepository.count();
    }
}


