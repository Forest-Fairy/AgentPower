package org.agentpower.infrastructure;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import org.agentpower.api.secure.decode.InputDecodeHandler;
import org.agentpower.api.secure.decode.InputDecodeRequired;
import org.agentpower.api.secure.encode.OutputEncodeHandler;
import org.agentpower.api.secure.encode.OutputEncodeRequired;
import org.agentpower.common.RSAUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ClientOutputEncodeHandler implements OutputEncodeHandler {
    @Override
    public Map<String, String> encode(Map<String, Object> params, OutputEncodeRequired encodeRequired, String algorithm, String publicKey) {
        Map<String, String> fieldsMapping = OutputEncodeHandler.toFieldsMapping(encodeRequired);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            Object o = params.get(key);
            if (ArrayUtil.isEmpty(o)) {
                continue;
            }
            result.put(mapping.getValue(), RSAUtil.encrypt(algorithm, JSON.toJSONString(o), publicKey));
        }
        return result;
    }
}
