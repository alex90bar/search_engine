package searchengine.utils;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import searchengine.dao.model.Index;
import searchengine.dao.model.Lemma;

/**
 * ContextUtils
 *
 * @author alex90bar
 */

public class ContextUtils {

    public static final Set<String> LINKS_SET = new ConcurrentSkipListSet<>();
    public static final Set<Index> INDEX_SET = new ConcurrentSkipListSet<>(Comparator.comparing(Index::toString));
    public static final Map<String, ConcurrentHashMap<String, Lemma>> LEMMA_MAP = new ConcurrentHashMap<>();
    public static final AtomicBoolean stopFlag = new AtomicBoolean(false);
    public static final AtomicBoolean isSinglePageIndexingRunning = new AtomicBoolean(false);

}


