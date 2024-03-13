package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * SearchResponse
 *
 * @author alex90bar
 */

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {

    private boolean result;
    private String error;
    private int count;
    private List<SearchData> data;

}