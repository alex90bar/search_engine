package searchengine.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.SiteDao;
import searchengine.dao.model.Index;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;

/**
 * SearchServiceImpl
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String EMPTY_QUERY_ERROR_MESS = "Задан пустой поисковый запрос";
    private static final String BAD_QUERY_ERROR_MESS = "По данным ключевым словам поиск невозможен";
    private static final String WRONG_SITE_ERROR_MESS = "Информация по сайту/сайтам отсутствует в БД";
    private static final String NOT_FOUND_BY_LEMMA_ERROR_MESS = "Не найдено страниц по данному слову: %s";

    private final LemmaProcessor lemmaProcessor;
    private final SiteDao siteDao;
    private final LemmaDao lemmaDao;
    private final IndexDao indexDao;

    @Override
    public ResponseEntity<SearchResponse> search(String query, String site, Integer offset, Integer limit) {
        if (StringUtils.isBlank(query)) {
            return ResponseEntity.badRequest().body(SearchResponse.builder().result(false).error(EMPTY_QUERY_ERROR_MESS).build());
        }

        offset = (offset == null || offset < 0) ? 0 : offset;
        limit = (limit == null || limit <= 0) ? 20 : limit;

        Map<String, Integer> lemmasMap = lemmaProcessor.extractLemmasFromContent(query);

        log.info("Леммы: {}", lemmasMap.keySet());

        if (lemmasMap.keySet().isEmpty()) {
            return ResponseEntity.badRequest().body(SearchResponse.builder().result(false).error(BAD_QUERY_ERROR_MESS).build());
        }

        List<SiteEntity> sites;
        if (StringUtils.isBlank(site)) {
            sites = siteDao.getAll();
            if (sites.isEmpty()) {
                return ResponseEntity.badRequest().body(SearchResponse.builder().result(false).error(WRONG_SITE_ERROR_MESS).build());
            }
        } else {
            SiteEntity siteEntity = siteDao.getByUrl(site);
            if (siteEntity == null) {
                return ResponseEntity.badRequest().body(SearchResponse.builder().result(false).error(WRONG_SITE_ERROR_MESS).build());
            }
            sites = Collections.singletonList(siteEntity);
        }

        for (SiteEntity siteEntity : sites) {

            ArrayList<Lemma> lemmaList = new ArrayList<>();

            for (String lemma : lemmasMap.keySet()) {
                Lemma byLemmaAndSite = lemmaDao.findByLemmaAndSite(lemma, siteEntity);
                if (byLemmaAndSite == null) {
                    return ResponseEntity.badRequest().body(
                        SearchResponse.builder()
                            .result(false)
                            .error(String.format(NOT_FOUND_BY_LEMMA_ERROR_MESS, lemma))
                            .build());
                }
                lemmaList.add(byLemmaAndSite);
            }

            if (!lemmaList.isEmpty()) {
                lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
            }

            log.info("Список лемм: {}", lemmaList);

            List<Index> allIndexes = new ArrayList<>();
            for (Lemma lemma : lemmaList) {
                List<Index> indexesByLemma = indexDao.findByLemma(lemma);
                allIndexes.addAll(indexesByLemma);
            }

            Map<Long, Long> pageIdCountMap = allIndexes.stream()
                .collect(Collectors.groupingBy(index -> index.getPage().getId(), Collectors.counting()));

            List<Index> filteredIndexes = allIndexes.stream()
                .filter(index -> pageIdCountMap.get(index.getPage().getId()) != null && pageIdCountMap.get(index.getPage().getId()) == lemmaList.size())
                .toList();

            log.info("Список индексов: {}", filteredIndexes.stream().map(Index::getId).collect(Collectors.toList()));
            log.info("Список значения rank по индексам: {}", filteredIndexes.stream().map(Index::getRank).collect(Collectors.toList()));
            log.info("Список страниц: {}", filteredIndexes.stream().map(Index::getPage).map(Page::getId).collect(Collectors.toList()));

            if (filteredIndexes.isEmpty()) {
                return ResponseEntity.ok().body(SearchResponse.builder().result(true).count(0).build());
            }

            Map<Page, Integer> pageAbsRelevanceMap = filteredIndexes.stream()
                .collect(Collectors.groupingBy(Index::getPage,
                    Collectors.mapping(Index::getRank, Collectors.summingInt(Integer::intValue))));

            log.info("Мапа абсолютной релевантности: {}", pageAbsRelevanceMap.values());
            Integer maxAbsRelevance = Collections.max(pageAbsRelevanceMap.values());
            log.info("Максимальная абсолютная релевантность: {}", maxAbsRelevance);

            List<SearchData> searchDataList = new ArrayList<>();
            for (Entry<Page, Integer> entry : pageAbsRelevanceMap.entrySet()) {
                Page page = entry.getKey();
                SearchData searchData = SearchData.builder()
                    .site(siteEntity.getUrl())
                    .siteName(siteEntity.getName())
                    .uri(page.getPath().substring(siteEntity.getUrl().length()))
                    .title(getPageTitle(page.getContent()))
                    .snippet(getPageSnippet(page.getContent(), lemmasMap.keySet()))
                    .relevance(calculateRelevance(maxAbsRelevance, entry.getValue()))
                    .build();
                searchDataList.add(searchData);
            }

            searchDataList.sort(Comparator.comparingDouble(SearchData::getRelevance).reversed());

            return ResponseEntity.ok().body(
                SearchResponse
                    .builder()
                    .result(true)
                    .count(searchDataList.size())
                    .data(searchDataList.stream().skip(offset).limit(limit).toList())
                    .build());

        }

        return null;
    }

    private double calculateRelevance(Integer maxAbsRelevance, Integer pageRelevance) {
        return pageRelevance.doubleValue() / maxAbsRelevance.doubleValue();
    }

    private String getPageSnippet(String pageContent, Set<String> lemmaSet) {
        /* TODO: написать метод для формирования сниппета */
        return String.format("Отрывок текста, содержащий леммы: %s", lemmaSet);
    }


    private String getPageTitle(String pageContent) {

        Document doc = Jsoup.parse(pageContent);

        Element titleElement = doc.select("title").first();
        if (titleElement != null) return titleElement.text();

        Elements h1Elements = doc.select("h1");
        if (!h1Elements.isEmpty()) {
            Element h1Element = h1Elements.first();
            String h1Title = h1Element.text();
            log.info("Заголовок в <h1>: {}", h1Title);
            return h1Title;
        }

        Element metaTitleElement = doc.select("meta[name=title]").first();
        if (metaTitleElement != null) {
            String metaTitle = metaTitleElement.attr("content");
            log.info("Заголовок в <meta name=title>: {}", metaTitle);
            return metaTitle;
        }

        return "Заголовок страницы отсутствует";
    }


}


