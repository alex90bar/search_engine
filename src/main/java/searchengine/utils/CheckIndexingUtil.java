package searchengine.utils;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.repository.SiteRepository;

/**
 * CheckIndexingUtil
 *
 * @author alex90bar
 */

@Service
@RequiredArgsConstructor
public class CheckIndexingUtil {

    private final SiteRepository siteRepository;

    public boolean checkIsIndexingRunning(List<Site> sites) {
        return sites.stream()
            .map(site -> siteRepository.findByUrl(site.getUrl()))
            .anyMatch(siteEntity -> siteEntity != null && IndexingStatus.INDEXING.equals(siteEntity.getStatus()));
    }

}
