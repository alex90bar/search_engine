package searchengine.services;

import searchengine.dto.search.SearchResponse;

/**
 * SearchService
 *
 * @author alex90bar
 */

public interface SearchService {

    SearchResponse search(String query, String site, Integer offset, Integer limit);
}
