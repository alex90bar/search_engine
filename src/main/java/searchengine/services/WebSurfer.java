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
    private final SiteDao siteDao;
    private final PageDao pageDao;
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

                pageDao.update(page);
                siteEntity.setStatusTime(ZonedDateTime.now());
                siteDao.update(siteEntity);

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
                tasks.add(new WebSurfer(link, siteDao, pageDao, siteEntity));
            }
        });

        return tasks;
    }
}


