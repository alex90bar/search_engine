package searchengine.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    private static final String BOLD_TAG_OPEN = "<b>";
    private static final String BOLD_TAG_CLOSE = "</b>";
    private static final int LETTERS_COUNT_IN_SNIPPET = 250;
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

    public String generateSnippet(String pageText, Set<String> lemmaSet) {
        String[] words = toArrayContainsRussianWords(pageText);
        List<String> snippets = new ArrayList<>();
        String pageTextInLowerCase = pageText.toLowerCase(Locale.ROOT);

        int lettersPerOneSnippet = LETTERS_COUNT_IN_SNIPPET / lemmaSet.size();

        for (String lemma : lemmaSet) {
            for (String currentWord : words) {
                List<String> normalForms = luceneMorphology.getNormalForms(currentWord);
                if (!normalForms.isEmpty()) {
                    String normalWord = normalForms.get(0);
                    if (lemma.equals(normalWord)) {
                        String snippet = extractSnippetFromTextAndMakeBold(pageText, pageTextInLowerCase, lettersPerOneSnippet, currentWord);
                        snippets.add(snippet);
                        break;
                    }
                }
            }
        }

        return String.join("...", snippets);

    }

    private String extractSnippetFromTextAndMakeBold(String pageText, String pageTextInLowerCase, int lettersPerOneSnippet, String currentWord) {
        int currentWordIndex = pageTextInLowerCase.indexOf(currentWord);
        int startIndex = Math.max(0, currentWordIndex - lettersPerOneSnippet / 2);
        int endIndex = Math.min(pageText.length() - 1, currentWordIndex + currentWord.length() + lettersPerOneSnippet / 2);

        StringBuilder snippetStringBuilder = new StringBuilder();

        String textBeforeCurrentWord = pageText.substring(startIndex, currentWordIndex);
        String currentWordFromText = pageText.substring(currentWordIndex, currentWordIndex + currentWord.length());
        String textAfterCurrentWord = pageText.substring(currentWordIndex + currentWord.length(), endIndex);

        snippetStringBuilder.append(textBeforeCurrentWord);
        snippetStringBuilder.append(BOLD_TAG_OPEN);
        snippetStringBuilder.append(currentWordFromText);
        snippetStringBuilder.append(BOLD_TAG_CLOSE);
        snippetStringBuilder.append(textAfterCurrentWord);
        return snippetStringBuilder.toString();
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
