package org.agentpower.common;

import org.apache.commons.lang3.StringUtils;

public class SearchUtils {
    private SearchUtils() {}
    public static int searchEach(String text, String query) {
        if (text == null || query == null) {
            return -1;
        }
        if (StringUtils.equalsIgnoreCase(text, query)) {
            return 0;
        }
        int startIndex = 0;
        for (char c : query.toCharArray()) {
            if (c == ' ') {
                continue;
            }
            if ((startIndex = StringUtils.indexOfIgnoreCase(text, String.valueOf(c), startIndex)) == -1) {
                return startIndex;
            }
        }
        return startIndex;
    }
}
