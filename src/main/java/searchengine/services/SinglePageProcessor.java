package searchengine.services;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dao.model.Index;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.dao.repository.LemmaRepository;
import searchengine.dao.repository.PageRepository;
import searchengine.dao.repository.SiteRepository;
import searchengine.utils.ContextUtils;
import searchengine.utils.DatabaseUtil;

/**
 * SinglePageProcessor
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SinglePageProcessor {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaProcessor lemmaProcessor;
    private final DatabaseUtil databaseUtil;

    public Document processSinglePage(String url, SiteEntity siteEntity, boolean isSingleIndexing) throws IOException {
        Connection.Response response = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
            .referrer("http://www.google.com")
            .execute();

        int statusCode = response.statusCode();

        Document document = response.parse();

        Page page = Page.builder()
            .site(siteEntity)
            .path(url)
            .code(statusCode)
            .content(document.html())
            .build();

        Page updatedPage;
        synchronized (lemmaRepository) {
            updatedPage = pageRepository.save(page);
            siteEntity.setStatusTime(ZonedDateTime.now());
            siteEntity.setStatus(IndexingStatus.INDEXING);
            siteRepository.save(siteEntity);
        }


        Map<String, Integer> lemmas = lemmaProcessor.extractLemmasFromContent(document.html());

        List<Lemma> lemmasForUpdate = new ArrayList<>();

        log.info("Найдено лемм: {} для странички: {}", lemmas.keySet().size(), url);

        List<Lemma> updatedLemmas;

        synchronized (lemmaRepository) {
        lemmas.keySet().forEach(lemmaText -> {

            Lemma lemma;
            if (isSingleIndexing) {
                lemma = lemmaRepository.findByLemmaAndSite(lemmaText, siteEntity);
            } else {
                lemma = ContextUtils.LEMMA_MAP
                    .get(siteEntity.getUrl())
                    .get(lemmaText);
            }

            if (lemma != null) {
                lemma.setFrequency(lemma.getFrequency() + 1);
            } else {
                lemma = Lemma.builder()
                    .lemma(lemmaText)
                    .frequency(1)
                    .site(siteEntity)
                    .build();
            }

            lemmasForUpdate.add(lemma);
        });

            updatedLemmas = lemmaRepository.saveAll(lemmasForUpdate);
            updatedLemmas.forEach(lemma -> ContextUtils.LEMMA_MAP
                .get(siteEntity.getUrl())
                .put(lemma.getLemma(), lemma));
        }

        List<Index> indexList = updatedLemmas.stream()
            .map(lemma -> Index.builder()
                .lemma(lemma)
                .page(updatedPage)
                .rank(lemmas.get(lemma.getLemma()))
                .build())
            .toList();
        ContextUtils.INDEX_SET.addAll(indexList);

        if (ContextUtils.INDEX_SET.size() > 10000 || isSingleIndexing) {
            List<Index> indices = ContextUtils.INDEX_SET.stream().toList();
            ContextUtils.INDEX_SET.clear();
            synchronized (lemmaRepository) {
                databaseUtil.updateIndexList(indices);
            }
        }

        log.info("Все леммы обработаны и сохранены для странички: {}", url);
        return document;
    }

}
