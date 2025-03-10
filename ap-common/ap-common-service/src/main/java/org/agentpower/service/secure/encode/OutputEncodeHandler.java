package org.agentpower.service.secure.encode;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.agentpower.common.RSAUtil;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

@Service
public class OutputEncodeHandler {
    static Map<String, String> toFieldsMapping(OutputEncodeRequired encodeRequired) {
        String[] fields = encodeRequired.fields();
        String[] projects = encodeRequired.projects();
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

    public void encode(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                       @NonNull HandlerMethod handlerMethod, @Nullable ModelAndView modelAndView,
                       @NonNull OutputEncodeRequired encodeRequired, String publicKey) {
        if (modelAndView == null) {
            return;
        }
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> fieldsMapping = OutputEncodeHandler.toFieldsMapping(encodeRequired);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            Object o = params.get(key);
            if (ArrayUtil.isEmpty(o)) {
                continue;
            }
            result.put(mapping.getValue(), RSAUtil.encrypt(RSAUtil.ALGORITHM, JSON.toJSONString(o), publicKey));
        }
        modelAndView.getModelMap().putAll(result);
    }
}
