package searchengine.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.PageRepository;

/**
 * PageDaoImpl
 *
 * @author alex90bar
 */

@Repository
@RequiredArgsConstructor
public class PageDaoImpl implements PageDao {

    private final PageRepository pageRepository;

    @Override
    @Transactional
    public long deleteAllBySite(SiteEntity site) {
        return pageRepository.deleteAllBySiteId(site);
    }

    @Override
    public boolean existsByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    @Override
    public Page update(Page page) {
        return pageRepository.save(page);
    }
}


