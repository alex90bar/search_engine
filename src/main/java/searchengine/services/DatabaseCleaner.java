package searchengine.services;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.utils.ContextUtils;

/**
 * DatabaseCleaner
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final SiteDao siteDao;
    private final PageDao pageDao;
    private final IndexDao indexDao;
    private final LemmaDao lemmaDao;
    private final ConcurrentWebSiteProcessor webSiteProcessor;

    @Async("taskExecutor")
    public void clearDataAndStartIndexing(List<Site> sites) {
        ContextUtils.isDatabaseCleanerWorking.set(true);

        long indexTotalCount = indexDao.getTotalCount();
        log.info("Начинаем очистку, удаляем индексы, количество: {} ...", indexTotalCount);
        indexDao.deleteAll();

        if (ContextUtils.stopFlag.get()) {
            ContextUtils.isDatabaseCleanerWorking.set(false);
            return;
        }

        long lemmaTotalCount = lemmaDao.getTotalCount();
        log.info("Удаляем леммы, количество: {} ...", lemmaTotalCount);
        lemmaDao.deleteAll();

        if (ContextUtils.stopFlag.get()) {
            ContextUtils.isDatabaseCleanerWorking.set(false);
            return;
        }

        long pageTotalCount = pageDao.getTotalCount();
        log.info("Удаляем странички, количество: {} ...", pageTotalCount);
        pageDao.deleteAll();

        if (ContextUtils.stopFlag.get()) {
            ContextUtils.isDatabaseCleanerWorking.set(false);
            return;
        }

        long siteTotalCount = siteDao.getTotalCount();
        log.info("Удаляем сайты, количество: {} ...", siteTotalCount);
        siteDao.deleteAll();

        ContextUtils.isDatabaseCleanerWorking.set(false);

        sites.forEach(webSiteProcessor::processWebSite);
    }
}