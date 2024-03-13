package searchengine.services;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dao.IndexDao;
import searchengine.dao.SiteDao;
import searchengine.dao.model.Index;
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
    private final IndexDao indexDao;
    private final SinglePageProcessor singlePageProcessor;

    @Async("taskExecutor")
    public void processWebSite(Site site) {

        log.info("Начинаем процесс индексации... сайт: {}", site.getName());

        SiteEntity siteEntity = saveSiteToDatabase(site);

        ContextUtils.LEMMA_MAP.put(siteEntity.getUrl(), new ConcurrentHashMap<>());

        startForkJoinPool(siteEntity);

        finishWebSiteProcessing(siteEntity);
    }

    public SiteEntity saveSiteToDatabase(Site site) {
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
        WebSurfer webSurfer = new WebSurfer(siteEntity.getUrl(), singlePageProcessor, siteEntity);
        forkJoinPool.invoke(webSurfer);
    }

    private void finishWebSiteProcessing(SiteEntity siteEntity) {
        if (ContextUtils.stopFlag.get()) {
            log.info("Останавливаем индексацию... сайт: {}", siteEntity.getName());
            siteDao.setStatusFailed(siteEntity);
        } else {
            siteEntity.setStatus(IndexingStatus.INDEXED);
            siteDao.update(siteEntity);
        }

        List<Index> indices = ContextUtils.INDEX_SET.stream().toList();
        ContextUtils.INDEX_SET.clear();
        synchronized (indexDao) {
            indexDao.updateList(indices);
        }

        log.info("Обработано ссылок всего: {} сайт {}", ContextUtils.LINKS_SET.size(), siteEntity.getName());
        log.info("Перечень ссылок: {}", ContextUtils.LINKS_SET);
    }

}