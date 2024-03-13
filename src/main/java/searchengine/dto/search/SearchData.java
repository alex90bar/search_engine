package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * SearchData
 *
 * @author alex90bar
 */

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchData {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;

}