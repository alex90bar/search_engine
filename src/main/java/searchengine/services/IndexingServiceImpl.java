package searchengine.services;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.SiteDao;
import searchengine.dao.model.IndexingStatus;
import searchengine.dto.indexing.IndexingResponse;
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
    private final SitesList sitesList;
    private final ConcurrentWebSiteProcessor webSiteProcessor;

    private static final String INDEXING_IS_RUNNING_MESS = "Индексация уже запущена";
    private static final String INDEXING_IS_NOT_RUNNING_MESS = "Индексация не запущена";

    @Override
    public ResponseEntity<IndexingResponse> startIndexing() {
        List<Site> sites = sitesList.getSites();

        if (checkIsIndexingRunning(sites)) {
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(INDEXING_IS_RUNNING_MESS).build());
        }

        ContextUtils.LINKS_SET.clear();
        ContextUtils.stopFlag.set(false);

        sites.forEach(webSiteProcessor::processWebSite);

        return ResponseEntity.ok(IndexingResponse.builder().result(true).build());
    }

    @Override
    public ResponseEntity<IndexingResponse> stopIndexing() {
        List<Site> sites = sitesList.getSites();

        if (!checkIsIndexingRunning(sites)) {
            return ResponseEntity.badRequest().body(IndexingResponse.builder().result(false).error(INDEXING_IS_NOT_RUNNING_MESS).build());
        }

        ContextUtils.stopFlag.set(true);

        changeDbStatusToFailed(sites);

        return ResponseEntity.ok(IndexingResponse.builder().result(true).build());
    }

    private boolean checkIsIndexingRunning(List<Site> sites) {
        return sites.stream()
            .map(site -> siteDao.getByUrl(site.getUrl()))
            .anyMatch(siteEntity -> siteEntity != null && IndexingStatus.INDEXING.equals(siteEntity.getStatus()));
    }

    private void changeDbStatusToFailed(List<Site> sites) {
        sites.stream()
            .map(site -> siteDao.getByUrl(site.getUrl()))
            .filter(siteEntity -> siteEntity != null && IndexingStatus.INDEXING.equals(siteEntity.getStatus()))
            .forEach(siteDao::setStatusFailed);
    }
}


