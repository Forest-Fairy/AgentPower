package org.agentpower.service.secure.codec;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.asymmetric.RSA;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import org.agentpower.common.RSAUtil;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;

import java.security.PrivateKey;
import java.util.*;

public class InputDecoder {

    private final Decoder decoder;

    public InputDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public void decode(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull HandlerMethod handlerMethod,
                       @NonNull InputDecodeRequired decodeRequired) {
        if (decoder == null) {
            return;
        }
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> fieldsMapping = CodecHelper.toFieldsMapping(decodeRequired);
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            String[] values = params.get(key);
            if (ArrayUtil.isEmpty(values)) {
                continue;
            }
            result.put(mapping.getValue(), new String[]{decoder.decodeToUtf8Str(values[0])});
        }
        result.forEach(request::setAttribute);
    }
}
