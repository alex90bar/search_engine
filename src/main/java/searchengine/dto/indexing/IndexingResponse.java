package searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * StartIndexingResponse
 *
 * @author alex90bar
 */

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexingResponse {

    private boolean result;
    private String error;
}