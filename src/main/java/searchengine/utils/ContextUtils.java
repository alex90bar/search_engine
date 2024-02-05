package searchengine.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ContextUtils
 *
 * @author alex90bar
 */

public class ContextUtils {

    public static final Set<String> LINKS_SET = new ConcurrentSkipListSet<>();
    public static final AtomicBoolean stopFlag = new AtomicBoolean(false);

}


