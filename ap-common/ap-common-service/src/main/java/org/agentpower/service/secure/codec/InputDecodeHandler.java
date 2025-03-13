package org.agentpower.service.secure.codec;

import cn.hutool.core.util.ArrayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;

import java.util.*;

@Service
public class InputDecodeHandler {

    public void decode(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull HandlerMethod handlerMethod,
                       @NonNull InputDecodeRequired decodeRequired) {
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> fieldsMapping = CodecHelper.toFieldsMapping(decodeRequired);
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            String[] values = params.get(key);
            if (ArrayUtil.isEmpty(values)) {
                continue;
            }
            result.put(mapping.getValue(), new String[]{CodecHelper.decode(values[0])});
        }
        result.forEach(request::setAttribute);
    }
}
