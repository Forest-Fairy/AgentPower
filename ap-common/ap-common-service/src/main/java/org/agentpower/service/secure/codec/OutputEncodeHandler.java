package org.agentpower.service.secure.codec;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import java.security.PublicKey;
import java.util.*;

@Service
public class OutputEncodeHandler {

    public void encode(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                       @NonNull HandlerMethod handlerMethod, @Nullable ModelAndView modelAndView,
                       @NonNull OutputEncodeRequired encodeRequired, PublicKey publicKey) {
        if (modelAndView == null) {
            return;
        }
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> fieldsMapping = CodecHelper.toFieldsMapping(encodeRequired);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            Object o = params.get(key);
            if (ArrayUtil.isEmpty(o)) {
                continue;
            }
            result.put(mapping.getValue(), CodecHelper.encode(JSON.toJSONString(o)));
        }
        modelAndView.getModelMap().putAll(result);
    }
}
