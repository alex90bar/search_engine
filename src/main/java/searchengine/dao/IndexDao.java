package searchengine.dao;

import java.util.List;
import searchengine.dao.model.Index;
import searchengine.dao.model.Page;

/**
 * IndexDao
 *
 * @author alex90bar
 */

public interface IndexDao {

    Index update(Index index);

    List<Index> updateList(List<Index> indexList);

    List<Index> findByPage(Page page);

    void deleteIndexList(List<Index> indexList);

    void deleteAll();

    long getTotalCount();
}
