package org.agentpower.common;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static boolean containsAnyTypes(String text, String delimiter, String... types) {
        if (text != null && types != null) {
            if (types.length == 0) {
                return true;
            }
            Set<String> typeSet = Arrays.stream(types)
                    .filter(StringUtils::isNotBlank)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            return Arrays.stream(StringUtils.isBlank(delimiter)
                            ? new String[]{text} : text.split(delimiter))
                    .filter(StringUtils::isNotBlank)
                    .map(String::toUpperCase)
                    .anyMatch(typeSet::contains);
        }
        return false;
    }
}
