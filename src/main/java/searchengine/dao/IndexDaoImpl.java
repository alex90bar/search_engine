package searchengine.dao;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dao.model.Index;
import searchengine.dao.model.Lemma;
import searchengine.dao.model.Page;
import searchengine.dao.repository.IndexRepository;

/**
 * IndexDaoImpl
 *
 * @author alex90bar
 */

@Slf4j
@Repository
@RequiredArgsConstructor
public class IndexDaoImpl implements IndexDao {

    private final IndexRepository indexRepository;

    @Override
    @Transactional
    public Index update(Index index) {
        return indexRepository.save(index);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Index> findByPage(Page page) {
        return indexRepository.findByPage(page);
    }

    @Override
    @Transactional
    public void deleteIndexList(List<Index> indexList) {
        indexRepository.deleteAll(indexList);
    }

    @Override
    @Transactional
    public List<Index> updateList(List<Index> indexList) {
        log.info("Сохраняем индексы, количество: {}", indexList.size());
        List<Index> indices = indexRepository.saveAll(indexList);
        log.info("Индексы сохранены");
        return indices;
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return indexRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Index> findByLemma(Lemma lemma) {
        return indexRepository.findByLemma(lemma);
    }
}
