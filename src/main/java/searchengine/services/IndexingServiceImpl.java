package searchengine.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.UnsupportedMimeTypeException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dao.model.Index;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.utils.CheckIndexingUtil;
import searchengine.utils.ContextUtils;

/**
 * IndexingServiceImpl
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteDao siteDao;
    private final PageDao pageDao;
    private final IndexDao indexDao;
    private final LemmaDao lemmaDao;
    private final SitesList sitesList;
    private final DatabaseCleaner databaseCleaner;
    private final ConcurrentWebSiteProcessor webSiteProcessor;
    private final SinglePageProcessor singlePageProcessor;
    private final CheckIndexingUtil checkIndexingUtil;

    private static final String INDEXING_IS_RUNNING_MESS = "Индексация уже запущена";
    private static final String INDEXING_IS_NOT_RUNNING_MESS = "Индексация не запущена";
    private static final String INCORRECT_URL_FOR_INDEXING_MESS = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";
    private static final String INCORRECT_URL_TYPE_FOR_INDEXING_MESS = "Не поддерживаемый тип гиперссылки, индексируем только html-странички";
    private static final String SINGLE_PAGE_INDEXING_ERROR_MESS = "Ошибка индексации странички";

    @Override
    public ResponseEntity<IndexingResponse> startIndexing() {
        List<Site> sites = sitesList.getSites();

        if (checkIndexingUtil.checkIsIndexingRunning(sites) || ContextUtils.isSinglePageIndexingRunning.get()) {
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(INDEXING_IS_RUNNING_MESS).build());
        }

        ContextUtils.LINKS_SET.clear();
        ContextUtils.INDEX_SET.clear();
        ContextUtils.LEMMA_MAP.clear();
        ContextUtils.stopFlag.set(false);

        databaseCleaner.clearDataAndStartIndexing();

        sites.forEach(webSiteProcessor::processWebSite);

        return ResponseEntity.ok(IndexingResponse.builder().result(true).build());
    }

    @Override
    public ResponseEntity<IndexingResponse> stopIndexing() {
        List<Site> sites = sitesList.getSites();

        if (!checkIndexingUtil.checkIsIndexingRunning(sites)) {
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(INDEXING_IS_NOT_RUNNING_MESS).build());
        }

        ContextUtils.stopFlag.set(true);

        changeDbStatusToFailed(sites);

        log.info("Получена команда на остановку индексации...");

        return ResponseEntity.ok(IndexingResponse.builder().result(true).build());
    }

    @Override
    public ResponseEntity<IndexingResponse> indexPage(String url) {
        List<Site> sites = sitesList.getSites();

        if (checkIndexingUtil.checkIsIndexingRunning(sites) || ContextUtils.isSinglePageIndexingRunning.get()) {
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(INDEXING_IS_RUNNING_MESS).build());
        }

        Site site = checkUrlForIndexing(url);
        if (site == null) {
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(INCORRECT_URL_FOR_INDEXING_MESS).build());
        }

        ContextUtils.isSinglePageIndexingRunning.set(true);

        SiteEntity siteEntity = siteDao.getByUrl(site.getUrl());
        if (siteEntity == null) {
            siteEntity = webSiteProcessor.saveSiteToDatabase(site);
        } else {
            if (pageDao.existsByPath(url)) {
                clearLemmasAndIndexesForPage(url);
            }
        }

        ContextUtils.LEMMA_MAP.put(site.getUrl(), new ConcurrentHashMap<>());

        try {
            singlePageProcessor.processSinglePage(url, siteEntity, true);
        } catch (UnsupportedMimeTypeException e) {
            log.debug("Не поддерживаемый тип гиперссылки, индексируем только html-странички, url: {} текст ошибки: {}", url, e.getMessage());
            ContextUtils.isSinglePageIndexingRunning.set(false);
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(INCORRECT_URL_TYPE_FOR_INDEXING_MESS).build());
        } catch (IOException e) {
            log.error("Ошибка индексации странички {} : {}", url, e.getMessage(), e);
            ContextUtils.isSinglePageIndexingRunning.set(false);
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(SINGLE_PAGE_INDEXING_ERROR_MESS).build());
        }

        siteDao.setStatusFailed(siteEntity);

        ContextUtils.isSinglePageIndexingRunning.set(false);
        return ResponseEntity.ok(IndexingResponse.builder().result(true).build());
    }

    private void clearLemmasAndIndexesForPage(String path) {
        log.info("Начинаем очистку таблиц page, lemma и index для странички: {}", path);
        Page page = pageDao.findByPath(path);
        List<Lemma> lemmaList = new ArrayList<>();
        List<Index> indexList = indexDao.findByPage(page);
        indexList.forEach(index -> lemmaList.add(index.getLemma()));
        indexDao.deleteIndexList(indexList);

        List<Lemma> lemmasToDelete = lemmaList.stream()
            .filter(lemma -> lemma.getFrequency() == 1)
            .toList();

        lemmaDao.deleteLemmaList(lemmasToDelete);

        List<Lemma> lemmasToUpdate = lemmaList.stream()
            .filter(lemma -> lemma.getFrequency() > 1)
            .map(lemma -> {
                lemma.setFrequency(lemma.getFrequency() - 1);
                return lemma;
            })
            .toList();

        lemmaDao.updateList(lemmasToUpdate);

        pageDao.delete(page);
        log.info("Очистка таблиц завершена для странички: {}", path);
    }

    private void changeDbStatusToFailed(List<Site> sites) {
        sites.stream()
            .map(site -> siteDao.getByUrl(site.getUrl()))
            .filter(siteEntity -> siteEntity != null && IndexingStatus.INDEXING.equals(siteEntity.getStatus()))
            .forEach(siteDao::setStatusFailed);
    }

    private Site checkUrlForIndexing(String url) {
        return sitesList.getSites()
            .stream()
            .filter(site -> url.startsWith(site.getUrl()))
            .findFirst()
            .orElse(null);
    }
}


