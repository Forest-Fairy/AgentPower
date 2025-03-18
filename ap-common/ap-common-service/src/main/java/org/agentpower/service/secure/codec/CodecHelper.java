package org.agentpower.service.secure.codec;

import java.util.*;

public class CodecHelper {
    public static Map<String, String> toFieldsMapping(OutputEncodeRequired encodeRequired) {
        if (encodeRequired != null) {
            String[] fields = encodeRequired.fields();
            String[] projects = encodeRequired.projects();
            return toFieldsMapping(fields, projects);
        }
        return Map.of();
    }
    public static Map<String, String> toFieldsMapping(InputDecodeRequired decodeRequired) {
        if (decodeRequired != null) {
            String[] fields = decodeRequired.fields();
            String[] projects = decodeRequired.projects();
            return toFieldsMapping(fields, projects);
        }
        return Map.of();
    }

    private static Map<String, String> toFieldsMapping(String[] fields, String[] projects) {
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
}
