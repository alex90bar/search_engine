package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.search.SearchResponse;

/**
 * SearchService
 *
 * @author alex90bar
 */

public interface SearchService {

    ResponseEntity<SearchResponse> search(String query, String site, Integer offset, Integer limit);
}
