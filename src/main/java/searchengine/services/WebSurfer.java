package searchengine.services;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dao.model.Page;
import searchengine.dao.model.SiteEntity;
import searchengine.utils.ContextUtils;

/**
 * WebSurfer
 *
 * @author alex90bar
 */

@Slf4j
@RequiredArgsConstructor
public class WebSurfer extends RecursiveAction {

    private final String url;
    private final SinglePageProcessor singlePageProcessor;
    private final SiteEntity siteEntity;

    @Override
    protected void compute() {
        try {
            log.debug("Мы сейчас на страничке: {}", url);

            if (ContextUtils.stopFlag.get()) {
                log.debug("Останавливаем индексацию... страничка: {}", url);
            } else if (ContextUtils.LINKS_SET.contains(url)) {
                log.debug("Страничка уже есть в БД: {}", url);
            } else {
                ContextUtils.LINKS_SET.add(url);

                Document document = singlePageProcessor.processSinglePage(url, siteEntity);

                Elements links = document.select("a[href]");

                List<WebSurfer> tasks = filterLinksAndCreateTasks(links);

                if (!tasks.isEmpty()) {
                    Thread.sleep(200);
                    invokeAll(tasks);
                }
            }

        } catch (UnsupportedMimeTypeException e) {
            log.debug("Не поддерживаемый тип гиперссылки, индексируем только html-странички, url: {} текст ошибки: {}", url, e.getMessage());
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка индексации странички {} : {}", url, e.getMessage(), e);
        }
    }

    private List<WebSurfer> filterLinksAndCreateTasks(Elements links) {
        Set<String> linksSet = new HashSet<>();

        for (Element link : links) {
            String nextUrl = link.absUrl("href");
            linksSet.add(nextUrl);
        }

        List<WebSurfer> tasks = new ArrayList<>();

        linksSet.forEach(link -> {
            if (ContextUtils.LINKS_SET.contains(link)) {
                log.debug("Страничка уже есть в БД: {}", link);
            }  else if (!link.startsWith(siteEntity.getUrl())) {
                log.debug("Сторонний сайт, пропускаем: {}", link);
            }  else {
                tasks.add(new WebSurfer(link, singlePageProcessor, siteEntity));
            }
        });

        return tasks;
    }
}


