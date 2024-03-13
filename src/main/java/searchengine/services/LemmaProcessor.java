package searchengine.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;

/**
 * LemmaProcessor
 *
 * @author alex90bar
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaProcessor {

    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorphology;

    public Map<String, Integer> extractLemmasFromContent(String content) {
        String[] words = toArrayContainsRussianWords(content);
        HashMap<String, Integer> lemmas = new HashMap<>();

        Arrays.stream(words)
            .filter(word -> !word.isBlank())
            .forEach(word -> {
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                return;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                return;
            }
            String normalWord = normalForms.get(0);
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        });

        return lemmas;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] toArrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
            .replaceAll("([^а-я\\s])", " ")
            .trim()
            .split("\\s+");
    }

}
