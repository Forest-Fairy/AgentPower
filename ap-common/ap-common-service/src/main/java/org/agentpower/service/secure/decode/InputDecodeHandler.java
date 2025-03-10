package org.agentpower.service.secure.decode;

import cn.hutool.core.util.ArrayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.agentpower.common.RSAUtil;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;

import java.util.*;

@Service
public class InputDecodeHandler {
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

    public void decode(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull HandlerMethod handlerMethod,
                @NonNull InputDecodeRequired decodeRequired, String privateKey) {
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> fieldsMapping = InputDecodeHandler.toFieldsMapping(decodeRequired);
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            String[] values = params.get(key);
            if (ArrayUtil.isEmpty(values)) {
                continue;
            }
            result.put(mapping.getValue(), new String[]{RSAUtil.decrypt(RSAUtil.ALGORITHM, values[0], privateKey)});
        }
        result.forEach(request::setAttribute);
    }
}
