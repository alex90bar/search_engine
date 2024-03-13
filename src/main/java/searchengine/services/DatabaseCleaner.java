package searchengine.services;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.utils.ContextUtils;

/**
 * DatabaseCleaner
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final SiteDao siteDao;
    private final PageDao pageDao;
    private final IndexDao indexDao;
    private final LemmaDao lemmaDao;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void clearDataAndStartIndexing() {

        long indexTotalCount = indexDao.getTotalCount();
        log.info("Начинаем очистку, удаляем индексы, количество: {} ...", indexTotalCount);
        truncateTable("`index`");

        long lemmaTotalCount = lemmaDao.getTotalCount();
        log.info("Удаляем леммы, количество: {} ...", lemmaTotalCount);
        truncateTable("lemma");

        long pageTotalCount = pageDao.getTotalCount();
        log.info("Удаляем странички, количество: {} ...", pageTotalCount);
        truncateTable("page");


        long siteTotalCount = siteDao.getTotalCount();
        log.info("Удаляем сайты, количество: {} ...", siteTotalCount);
        truncateTable("site");
    }

    private void truncateTable(String tableName) {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}