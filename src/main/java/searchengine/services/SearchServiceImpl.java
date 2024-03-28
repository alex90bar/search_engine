package searchengine.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
import org.springframework.stereotype.Service;
import searchengine.dao.model.Index;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.IndexRepository;
import searchengine.dao.repository.LemmaRepository;
import searchengine.dao.repository.SiteRepository;
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
    private static final String NOT_FOUND_BY_LEMMA_ERROR_MESS = "Не найдено страниц по словам: %s";

    private final LemmaProcessor lemmaProcessor;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        if (StringUtils.isBlank(query))
            return generateErrorResponse(EMPTY_QUERY_ERROR_MESS);

        offset = calculateOffset(offset);
        limit = calculateLimit(limit);

        Map<String, Integer> lemmasFromQueryMap = lemmaProcessor.extractLemmasFromContent(query);
        Set<String> lemmasFromQuerySet = lemmasFromQueryMap.keySet();

        log.info("Леммы: {}", lemmasFromQuerySet);

        if (lemmasFromQuerySet.isEmpty())
            return generateErrorResponse(BAD_QUERY_ERROR_MESS);

        List<SiteEntity> sites = prepareSitesData(site);

        if (sites.isEmpty())
            return generateErrorResponse(WRONG_SITE_ERROR_MESS);

        Set<String> lemmasNotFound = new HashSet<>();

        List<SearchData> searchDataResultList = searchDataByLemmasAndSites(lemmasNotFound, sites, lemmasFromQuerySet);

        if (searchDataResultList.isEmpty()) {
            if (lemmasNotFound.isEmpty()) {
                return SearchResponse.builder().result(true).count(0).build();
            } else {
                return generateErrorResponse(String.format(NOT_FOUND_BY_LEMMA_ERROR_MESS, String.join( ", ", lemmasNotFound)));
            }
        }

        sortResultList(searchDataResultList);

        return SearchResponse
            .builder()
            .result(true)
            .count(searchDataResultList.size())
            .data(searchDataResultList.stream().skip(offset).limit(limit).toList())
            .build();
    }

    private SearchResponse generateErrorResponse(String error) {
        return SearchResponse.builder().result(false).error(error).build();
    }

    private Integer calculateLimit(Integer limit) {
        return limit == null || limit <= 0 ? 20 : limit;
    }

    private Integer calculateOffset(Integer offset) {
        return offset == null || offset < 0 ? 0 : offset;
    }

    private List<SiteEntity> prepareSitesData(String site) {
        List<SiteEntity> sites;
        if (StringUtils.isBlank(site)) {
            sites = siteRepository.findAll();
            if (sites.isEmpty()) {
                return Collections.emptyList();
            }
        } else {
            SiteEntity siteEntity = siteRepository.findByUrl(site);
            if (siteEntity == null) {
                return Collections.emptyList();
            }
            sites = Collections.singletonList(siteEntity);
        }
        return sites;
    }

    private List<SearchData> searchDataByLemmasAndSites(Set<String> lemmasNotFound, List<SiteEntity> sites, Set<String> lemmasFromQuerySet) {
        List<SearchData> searchDataResultList = new ArrayList<>();
        log.info("Список сайтов для поиска: {}", sites.stream().map(SiteEntity::getUrl).toList());

        for (SiteEntity siteEntity : sites) {
            log.info("Ищем по сайту: {}", siteEntity.getName());

            ArrayList<Lemma> lemmaList = new ArrayList<>();

            findLemmasBySiteInDb(lemmasNotFound, lemmasFromQuerySet, siteEntity, lemmaList);

            /* проверка, найдены ли все слова из поискового запроса */
            if (lemmasFromQuerySet.size() == lemmaList.size()) {
                lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));

                log.debug("Список лемм: {}", lemmaList);

                List<Index> filteredIndexes = findIndexesByLemmaAndFilterIndexesByPage(lemmaList);

                if (!filteredIndexes.isEmpty()) {

                    searchDataResultList.addAll(calculateAbsoluteRelevanceAndGenerateResultList(filteredIndexes, siteEntity, lemmasFromQuerySet));

                }
            }
        }
        return searchDataResultList;
    }

    private void findLemmasBySiteInDb(Set<String> lemmasNotFound, Set<String> lemmasKeySet, SiteEntity siteEntity, ArrayList<Lemma> lemmaList) {
        for (String lemma : lemmasKeySet) {
            Lemma byLemmaAndSite = lemmaRepository.findByLemmaAndSite(lemma, siteEntity);
            if (byLemmaAndSite == null) {
                lemmasNotFound.add(lemma);
            } else {
                lemmaList.add(byLemmaAndSite);
            }
        }
    }

    /**
     * Метод производит поиск индексов по списку лемм,
     * считает количество страниц, на которой встречается каждый из индексов,
     * фильтрует список индексов,
     * оставляет только те страницы, на которых встречаются все леммы из списка.
     *
     * Если после поиска не остаётся таких страниц - возвращает пустой список.
     *
     * @param lemmaList список лемм
     * @return список индексов List<Index>
     *
     * */
    private List<Index> findIndexesByLemmaAndFilterIndexesByPage(List<Lemma> lemmaList) {
        List<Index> allIndexes = new ArrayList<>();
        for (Lemma lemma : lemmaList) {
            List<Index> indexesByLemma = indexRepository.findByLemma(lemma);
            allIndexes.addAll(indexesByLemma);
        }

        Map<Long, Long> pageIdCountMap = allIndexes.stream()
            .collect(Collectors.groupingBy(index -> index.getPage().getId(), Collectors.counting()));

        /* фильтруем индексы, оставляем только те страницы, в который есть каждая из лемм списка */
        List<Index> filteredIndexes = allIndexes.stream()
            .filter(index -> pageIdCountMap.get(index.getPage().getId()) != null && pageIdCountMap.get(index.getPage().getId()) == lemmaList.size())
            .toList();

        log.debug("Список индексов: {}", filteredIndexes.stream().map(Index::getId).toList());
        log.debug("Список значения rank по индексам: {}", filteredIndexes.stream().map(Index::getRank).toList());
        log.debug("Список страниц: {}", filteredIndexes.stream().map(Index::getPage).map(Page::getId).toList());
        return filteredIndexes;
    }

    private List<SearchData> calculateAbsoluteRelevanceAndGenerateResultList(List<Index> filteredIndexes, SiteEntity siteEntity, Set<String> lemmasKeySet) {
        List<SearchData> searchDataResultList = new ArrayList<>();
        Map<Page, Integer> pageAbsRelevanceMap = filteredIndexes.stream()
            .collect(Collectors.groupingBy(Index::getPage,
                Collectors.mapping(Index::getRank, Collectors.summingInt(Integer::intValue))));

        log.debug("Мапа абсолютной релевантности: {}", pageAbsRelevanceMap.values());
        Integer maxAbsRelevance = Collections.max(pageAbsRelevanceMap.values());
        log.debug("Максимальная абсолютная релевантность: {}", maxAbsRelevance);

        for (Entry<Page, Integer> entry : pageAbsRelevanceMap.entrySet()) {
            Page page = entry.getKey();
            SearchData searchData = SearchData.builder()
                .site(siteEntity.getUrl())
                .siteName(siteEntity.getName())
                .uri(page.getPath().substring(siteEntity.getUrl().length()))
                .title(getPageTitle(page.getContent()))
                .snippet(getPageSnippet(page.getContent(), lemmasKeySet))
                .relevance(calculateRelevance(maxAbsRelevance, entry.getValue()))
                .build();
            searchDataResultList.add(searchData);
        }
        return searchDataResultList;
    }

    private double calculateRelevance(Integer maxAbsRelevance, Integer pageRelevance) {
        return pageRelevance.doubleValue() / maxAbsRelevance.doubleValue();
    }

    private String getPageSnippet(String pageContent, Set<String> lemmaSet) {
        Document doc = Jsoup.parse(pageContent);
        String docBody = doc.text();
        return lemmaProcessor.generateSnippet(docBody, lemmaSet);
    }


    private String getPageTitle(String pageContent) {

        Document doc = Jsoup.parse(pageContent);

        Element titleElement = doc.select("title").first();
        if (titleElement != null) return titleElement.text();

        Elements h1Elements = doc.select("h1");
        if (!h1Elements.isEmpty()) {
            Element h1Element = h1Elements.first();
            String h1Title = h1Element.text();
            log.debug("Заголовок в <h1>: {}", h1Title);
            return h1Title;
        }

        Element metaTitleElement = doc.select("meta[name=title]").first();
        if (metaTitleElement != null) {
            String metaTitle = metaTitleElement.attr("content");
            log.debug("Заголовок в <meta name=title>: {}", metaTitle);
            return metaTitle;
        }

        return "Заголовок страницы отсутствует";
    }

    private void sortResultList(List<SearchData> searchDataResultList) {
        searchDataResultList.sort(Comparator.comparingDouble(SearchData::getRelevance).reversed());
    }

}


