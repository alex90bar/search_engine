package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dao.repository.IndexRepository;
import searchengine.dao.repository.LemmaRepository;
import searchengine.dao.repository.PageRepository;
import searchengine.dao.repository.SiteRepository;

/**
 * DatabaseCleaner
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void clearDataAndStartIndexing() {

        long indexTotalCount = indexRepository.count();
        log.info("Начинаем очистку, удаляем индексы, количество: {} ...", indexTotalCount);
        truncateTable("`index`");

        long lemmaTotalCount = lemmaRepository.count();
        log.info("Удаляем леммы, количество: {} ...", lemmaTotalCount);
        truncateTable("lemma");

        long pageTotalCount = pageRepository.count();
        log.info("Удаляем странички, количество: {} ...", pageTotalCount);
        truncateTable("page");


        long siteTotalCount = siteRepository.count();
        log.info("Удаляем сайты, количество: {} ...", siteTotalCount);
        truncateTable("site");
    }

    private void truncateTable(String tableName) {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}