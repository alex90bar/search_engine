package searchengine.services;

import java.time.ZonedDateTime;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.model.SiteEntity;
import searchengine.utils.ContextUtils;

/**
 * ConcurrentWebSiteProcessor
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrentWebSiteProcessor {

    private final SiteDao siteDao;
    private final PageDao pageDao;

    @Async("taskExecutor")
    public void processWebSite(Site site) {

        clearPagesAndSiteData(site);

        SiteEntity siteEntityNew = saveSiteToDatabase(site);

        startForkJoinPool(siteEntityNew);

        finishWebSiteProcessing(siteEntityNew);
    }

    private void clearPagesAndSiteData(Site site) {
        log.info("Начинаем процесс индексации... сайт: {}", site.getName());

        SiteEntity siteEntity = siteDao.getByUrl(site.getUrl());

        if (siteEntity != null) {
            long deletedPages = pageDao.deleteAllBySite(siteEntity);
            siteDao.deleteById(siteEntity.getId());
            log.info("Удалено страничек: {}", deletedPages);
        }
    }

    private SiteEntity saveSiteToDatabase(Site site) {
        SiteEntity siteEntity = SiteEntity.builder()
            .status(IndexingStatus.INDEXING)
            .statusTime(ZonedDateTime.now())
            .url(site.getUrl())
            .name(site.getName())
            .build();

        return siteDao.update(siteEntity);
    }

    private void startForkJoinPool(SiteEntity siteEntity) {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        WebSurfer webSurfer = new WebSurfer(siteEntity.getUrl(), siteDao, pageDao, siteEntity);
        forkJoinPool.invoke(webSurfer);
    }

    private void finishWebSiteProcessing(SiteEntity siteEntity) {
        if (ContextUtils.stopFlag.get()) {
            log.info("Останавливаем индексацию... сайт: {}", siteEntity.getName());
            siteDao.setStatusFailed(siteEntity);
        } else {
            siteEntity.setStatus(IndexingStatus.INDEXED);
            siteDao.update(siteEntity);

            log.info("Обработано ссылок всего: {}", ContextUtils.LINKS_SET.size());
            log.info("Перечень ссылок: {}", ContextUtils.LINKS_SET);
        }
    }

}


