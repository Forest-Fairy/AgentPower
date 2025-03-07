package org.agentpower.api.secure.decode;

import java.util.*;

public interface InputDecodeHandler {
    static Map<String, String> toFieldsMapping(InputDecodeRequired decodeRequired) {
        String[] fields = decodeRequired.fields();
        String[] projects = decodeRequired.projects();
        if (fields.length == 0) {
            return Map.of();
        }
        if (projects.length == 0) {
            projects = fields;
        } else if (projects.length != fields.length) {
            throw new IllegalArgumentException("定义了映射字段，则长度需要与解析字段长度一致");
        }
        List<AbstractMap.SimpleEntry<String, String>> entries = new ArrayList<>(fields.length);
        for (int i = 0; i < fields.length; i++) {
            entries.add(new AbstractMap.SimpleEntry<>(fields[i], projects[i] == null || projects[i].isEmpty() ? fields[i] : projects[i]));
        }
        return Map.ofEntries(entries.toArray(new AbstractMap.SimpleEntry[0]));
    }

    Map<String, String[]> decode(Map<String, String[]> params, InputDecodeRequired decodeRequired,
                               String algorithm, String privateKey);
}
