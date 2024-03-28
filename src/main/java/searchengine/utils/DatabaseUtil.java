package searchengine.utils;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dao.model.Index;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.IndexRepository;
import searchengine.dao.repository.SiteRepository;

/**
 * DatabaseUtil
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseUtil {

    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    private static final String INDEXING_IS_STOPPED_BY_USER_MESS = "Индексация остановлена пользователем";

    public void updateIndexList(List<Index> indexList) {
        log.info("Сохраняем индексы, количество: {}", indexList.size());
        indexRepository.saveAll(indexList);
        log.info("Индексы сохранены");
    }

    public void setStatusFailedForSite(SiteEntity siteEntity) {
        siteEntity.setStatus(IndexingStatus.FAILED);
        siteEntity.setLastError(INDEXING_IS_STOPPED_BY_USER_MESS);
        siteRepository.save(siteEntity);
    }

}