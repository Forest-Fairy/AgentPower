package org.poweragent.test;

import info.debatty.java.stringsimilarity.JaroWinkler;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StringSimilarityTest {

    @Test
    public void functionNameFilter() {
        List<String> functionNames = List.of(
                "read-file", "read-doecs", "result", "rapid", "flavor");
        String queryText = "e";
        System.out.println(queryText);
    }

}
