package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

/**
 * IndexingService
 *
 * @author alex90bar
 */

public interface IndexingService {

    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse indexPage(String url);
}


