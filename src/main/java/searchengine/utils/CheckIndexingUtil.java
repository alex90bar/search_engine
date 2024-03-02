package searchengine.utils;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dao.SiteDao;
import searchengine.dao.model.IndexingStatus;

/**
 * CheckIndexingUtil
 *
 * @author alex90bar
 */

@Service
@RequiredArgsConstructor
public class CheckIndexingUtil {

    private final SiteDao siteDao;

    public boolean checkIsIndexingRunning(List<Site> sites) {
        return sites.stream()
            .map(site -> siteDao.getByUrl(site.getUrl()))
            .anyMatch(siteEntity -> siteEntity != null && IndexingStatus.INDEXING.equals(siteEntity.getStatus()));
    }

}
