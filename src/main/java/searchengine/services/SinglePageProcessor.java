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
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dao.model.Index;
import searchengine.dao.model.IndexingStatus;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.utils.ContextUtils;

/**
 * SinglePageProcessor
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SinglePageProcessor {

    private final SiteDao siteDao;
    private final PageDao pageDao;
    private final LemmaProcessor lemmaProcessor;
    private final LemmaDao lemmaDao;
    private final IndexDao indexDao;

    public Document processSinglePage(String url, SiteEntity siteEntity) throws IOException {
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
        synchronized (lemmaDao) {
            updatedPage = pageDao.update(page);
            siteEntity.setStatusTime(ZonedDateTime.now());
            siteEntity.setStatus(IndexingStatus.INDEXING);
            siteDao.update(siteEntity);
        }


        Map<String, Integer> lemmas = lemmaProcessor.extractLemmasFromContent(document.html());

        List<Lemma> lemmasForUpdate = new ArrayList<>();

        log.info("Найдено лемм: {} для странички: {}", lemmas.keySet().size(), url);

        lemmas.keySet().forEach(lemmaText -> {
//            Lemma lemma = lemmaDao.findByLemmaAndSite(lemmaText, siteEntity);
            Lemma lemma = ContextUtils.LEMMA_MAP
                                        .get(siteEntity.getUrl())
                                        .get(lemmaText);

            if (lemma != null) {
                lemma.setFrequency(lemma.getFrequency() + 1);
            } else {
                lemma = Lemma.builder()
                    .lemma(lemmaText)
                    .frequency(1)
                    .site(siteEntity)
                    .build();
            }

//            Lemma updatedLemma = lemmaDao.update(lemma);
            lemmasForUpdate.add(lemma);

//            Index index = Index.builder()
//                .lemma(updatedLemma)
//                .page(updatedPage)
//                .rank(lemmas.get(lemmaText).floatValue())
//                .build();
//
//            indexDao.update(index);
        });
        List<Lemma> updatedLemmas;
        synchronized (lemmaDao) {
            updatedLemmas = lemmaDao.updateList(lemmasForUpdate);
        }

        List<Index> indexList = updatedLemmas.stream()
            .map(lemma -> Index.builder()
                .lemma(lemma)
                .page(updatedPage)
                .rank(lemmas.get(lemma.getLemma()).floatValue())
                .build())
            .toList();
        ContextUtils.INDEX_SET.addAll(indexList);

        if (ContextUtils.INDEX_SET.size() > 10000) {
            List<Index> indices = ContextUtils.INDEX_SET.stream().toList();
            ContextUtils.INDEX_SET.clear();
            synchronized (lemmaDao) {
                indexDao.updateList(indices);
            }
        }


        updatedLemmas.forEach(lemma -> ContextUtils.LEMMA_MAP
            .get(siteEntity.getUrl())
            .put(lemma.getLemma(), lemma));

        log.info("Все леммы обработаны и сохранены для странички: {}", url);
        return document;
    }

}
