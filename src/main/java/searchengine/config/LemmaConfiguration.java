package searchengine.config;

import java.io.IOException;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LemmaConfiguration
 *
 * @author alex90bar
 */

@Configuration
public class LemmaConfiguration {

    @Bean
    public LuceneMorphology luceneMorphology() throws IOException {
        return new RussianLuceneMorphology();
    }
}
