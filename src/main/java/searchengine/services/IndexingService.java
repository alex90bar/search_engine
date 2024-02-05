package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.indexing.IndexingResponse;

/**
 * IndexingService
 *
 * @author alex90bar
 */

public interface IndexingService {

    ResponseEntity<IndexingResponse> startIndexing();

    ResponseEntity<IndexingResponse> stopIndexing();
}


