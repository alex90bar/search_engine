package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import searchengine.config.SitesList;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dao.model.SiteEntity;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.util.ArrayList;
import java.util.List;
import searchengine.utils.CheckIndexingUtil;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final CheckIndexingUtil checkIndexingUtil;
    private final PageDao pageDao;
    private final LemmaDao lemmaDao;
    private final SiteDao siteDao;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        List<SiteEntity> allSites = siteDao.getAll();
        total.setSites(allSites.size());
        total.setPages(Math.toIntExact(pageDao.getTotalCount()));
        total.setLemmas(Math.toIntExact(lemmaDao.getTotalCount()));
        total.setIndexing(checkIndexingUtil.checkIsIndexingRunning(sites.getSites()));


        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        allSites.forEach(siteEntity -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());
            item.setStatus(siteEntity.getStatus().name());
            item.setStatusTime(siteEntity.getStatusTime().toEpochSecond() * 1000);
            item.setError(StringUtils.defaultString(siteEntity.getLastError()));
            item.setPages(pageDao.countPagesBySite(siteEntity));
            item.setLemmas(lemmaDao.countLemmasBySite(siteEntity));
            detailed.add(item);
        });

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
