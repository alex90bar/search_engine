package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import searchengine.config.SitesList;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.LemmaRepository;
import searchengine.dao.repository.PageRepository;
import searchengine.dao.repository.SiteRepository;
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
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        List<SiteEntity> allSites = siteRepository.findAll();
        total.setSites(allSites.size());
        total.setPages(Math.toIntExact(pageRepository.count()));
        total.setLemmas(Math.toIntExact(lemmaRepository.count()));
        total.setIndexing(checkIndexingUtil.checkIsIndexingRunning(sites.getSites()));


        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        allSites.forEach(siteEntity -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());
            item.setStatus(siteEntity.getStatus().name());
            item.setStatusTime(siteEntity.getStatusTime().toEpochSecond() * 1000);
            item.setError(StringUtils.defaultString(siteEntity.getLastError()));
            item.setPages(pageRepository.countPagesBySite(siteEntity));
            item.setLemmas(lemmaRepository.countLemmaBySite(siteEntity));
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
