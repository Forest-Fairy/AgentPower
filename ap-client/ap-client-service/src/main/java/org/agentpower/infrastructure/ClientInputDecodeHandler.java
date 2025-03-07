package org.agentpower.infrastructure;

import cn.hutool.core.util.ArrayUtil;
import org.agentpower.api.secure.decode.InputDecodeHandler;
import org.agentpower.api.secure.decode.InputDecodeRequired;
import org.agentpower.common.RSAUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ClientInputDecodeHandler implements InputDecodeHandler {
    @Override
    public Map<String, String[]> decode(Map<String, String[]> params, InputDecodeRequired decodeRequired, String algorithm, String privateKey) {
        Map<String, String> fieldsMapping = InputDecodeHandler.toFieldsMapping(decodeRequired);
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            String[] values = params.get(key);
            if (ArrayUtil.isEmpty(values)) {
                continue;
            }
            result.put(mapping.getValue(), new String[]{RSAUtil.decrypt(algorithm, values[0], privateKey)});
        }
        return result;
    }
}
