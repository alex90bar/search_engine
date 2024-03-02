package searchengine.dao;

import java.util.List;
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
        return pageRepository.deleteAllBySite(site);
    }

    @Override
    @Transactional
    public void deleteAll() {
        pageRepository.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Page> findAllBySite(SiteEntity site) {
        return pageRepository.findAllBySite(site);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    @Override
    @Transactional
    public Page update(Page page) {
        return pageRepository.save(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page findByPath(String path) {
        return pageRepository.findByPath(path);
    }

    @Override
    @Transactional
    public void delete(Page page) {
        pageRepository.delete(page);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return pageRepository.count();
    }
}


