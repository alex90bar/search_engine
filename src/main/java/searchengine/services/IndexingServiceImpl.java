package searchengine.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.UnsupportedMimeTypeException;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.model.Index;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.IndexRepository;
import searchengine.dao.repository.LemmaRepository;
import searchengine.dao.repository.PageRepository;
import searchengine.dao.repository.SiteRepository;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.utils.CheckIndexingUtil;
import searchengine.utils.ContextUtils;
import searchengine.utils.DatabaseUtil;

/**
 * IndexingServiceImpl
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final DatabaseUtil databaseUtil;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
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
    public IndexingResponse startIndexing() {
        List<Site> sites = sitesList.getSites();

        if (checkIndexingUtil.checkIsIndexingRunning(sites) || ContextUtils.isSinglePageIndexingRunning.get()) {
            return generateErrorResponse(INDEXING_IS_RUNNING_MESS);
        }

        ContextUtils.LINKS_SET.clear();
        ContextUtils.INDEX_SET.clear();
        ContextUtils.LEMMA_MAP.clear();
        ContextUtils.stopFlag.set(false);
        ContextUtils.isFullIndexingStarted.set(true);

        databaseCleaner.clearDataAndStartIndexing();

        sites.forEach(webSiteProcessor::processWebSite);

        return generateSuccessResponse();
    }

    @Override
    public IndexingResponse stopIndexing() {
        List<Site> sites = sitesList.getSites();

        if (!checkIndexingUtil.checkIsIndexingRunning(sites)) {
            return generateErrorResponse(INDEXING_IS_NOT_RUNNING_MESS);
        }

        ContextUtils.stopFlag.set(true);

        /* условие срабатывает при экстренном закрытии программы во время индексации, устанавливает для сайтов статус FAILED */
        if (!ContextUtils.isFullIndexingStarted.get()) {
            changeDbStatusToFailed(sites);
        }

        log.info("Получена команда на остановку индексации...");

        return generateSuccessResponse();
    }

    @Override
    public IndexingResponse indexPage(String url) {
        List<Site> sites = sitesList.getSites();

        if (checkIndexingUtil.checkIsIndexingRunning(sites) || ContextUtils.isSinglePageIndexingRunning.get()) {
            return generateErrorResponse(INDEXING_IS_RUNNING_MESS);
        }

        Site site = checkUrlForIndexing(url);
        if (site == null) {
            return generateErrorResponse(INCORRECT_URL_FOR_INDEXING_MESS);
        }

        ContextUtils.isSinglePageIndexingRunning.set(true);

        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
        if (siteEntity == null) {
            siteEntity = webSiteProcessor.saveSiteToDatabase(site);
        } else {
            if (pageRepository.existsByPath(url)) {
                clearLemmasAndIndexesForPage(url);
            }
        }

        ContextUtils.LEMMA_MAP.put(site.getUrl(), new ConcurrentHashMap<>());

        try {
            singlePageProcessor.processSinglePage(url, siteEntity, true);
        } catch (UnsupportedMimeTypeException e) {
            log.debug("Не поддерживаемый тип гиперссылки, индексируем только html-странички, url: {} текст ошибки: {}", url, e.getMessage());
            ContextUtils.isSinglePageIndexingRunning.set(false);
            return generateErrorResponse(INCORRECT_URL_TYPE_FOR_INDEXING_MESS);
        } catch (IOException e) {
            log.error("Ошибка индексации странички {} : {}", url, e.getMessage(), e);
            ContextUtils.isSinglePageIndexingRunning.set(false);
            return generateErrorResponse(SINGLE_PAGE_INDEXING_ERROR_MESS);
        }

        databaseUtil.setStatusFailedForSite(siteEntity);

        ContextUtils.isSinglePageIndexingRunning.set(false);
        return generateSuccessResponse();
    }

    private void clearLemmasAndIndexesForPage(String path) {
        log.info("Начинаем очистку таблиц page, lemma и index для странички: {}", path);
        Page page = pageRepository.findByPath(path);
        List<Lemma> lemmaList = new ArrayList<>();
        List<Index> indexList = indexRepository.findByPage(page);
        indexList.forEach(index -> lemmaList.add(index.getLemma()));
        indexRepository.deleteAll(indexList);

        List<Lemma> lemmasToDelete = lemmaList.stream()
            .filter(lemma -> lemma.getFrequency() == 1)
            .toList();

        lemmaRepository.deleteAll(lemmasToDelete);

        List<Lemma> lemmasToUpdate = lemmaList.stream()
            .filter(lemma -> lemma.getFrequency() > 1)
            .map(lemma -> {
                lemma.setFrequency(lemma.getFrequency() - 1);
                return lemma;
            })
            .toList();

        lemmaRepository.saveAll(lemmasToUpdate);

        pageRepository.delete(page);
        log.info("Очистка таблиц завершена для странички: {}", path);
    }

    private void changeDbStatusToFailed(List<Site> sites) {
        sites.stream()
            .map(site -> siteRepository.findByUrl(site.getUrl()))
            .filter(siteEntity -> siteEntity != null && IndexingStatus.INDEXING.equals(siteEntity.getStatus()))
            .forEach(databaseUtil::setStatusFailedForSite);
    }

    private Site checkUrlForIndexing(String url) {
        return sitesList.getSites()
            .stream()
            .filter(site -> url.startsWith(site.getUrl()))
            .findFirst()
            .orElse(null);
    }

    private IndexingResponse generateErrorResponse(String error) {
        return IndexingResponse.builder().result(false).error(error).build();
    }

    private IndexingResponse generateSuccessResponse() {
        return IndexingResponse.builder().result(true).build();
    }

}


