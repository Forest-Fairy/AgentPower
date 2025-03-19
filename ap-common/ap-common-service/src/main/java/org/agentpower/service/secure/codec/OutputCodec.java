package org.agentpower.service.secure.codec;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.agentpower.api.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

@Getter
public class OutputCodec {
    /** 登陆等请求生成token时需要编码器 */
    private final Encoder encoder;
    /** 请求方如需加密 则利用接收方的公钥对请求头里的公钥加密 因此需要接收方的解码器来解密 */
    private final Decoder decoder;

    public OutputCodec(Encoder encoder, Decoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * 加密(接收方公钥，发起方公钥) => 密文
     * 解密(接收方私钥，密文) => 发起方公钥
     */
    public void encode(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull HandlerMethod handlerMethod,
                       @Nullable ModelAndView modelAndView,
                       @NonNull OutputEncodeRequired encodeRequired) {
        if (modelAndView == null) {
            return;
        }
        // 从请求头获取发起方公钥 构建 Encoder
        String publicKey = Optional.ofNullable(request.getHeader(Constants.Header.ENCODED_KEY))
                .filter(StringUtils::isNotBlank)
                .map(decoder::decodeToUtf8Str)
                .orElse(null);
        if (StringUtils.isBlank(publicKey)) {
            // 请求头未传入公钥则明文传输
            return;
        }
        String algorithm = request.getHeader(Constants.Header.ALGORITHM);
        Encoder encoder = CodecProvider.GenerateEncoder(algorithm, publicKey);
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> fieldsMapping = CodecHelper.toFieldsMapping(encodeRequired);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> mapping : fieldsMapping.entrySet()) {
            String key = mapping.getKey();
            Object o = params.get(key);
            if (ArrayUtil.isEmpty(o)) {
                continue;
            }
            result.put(mapping.getValue(), encoder.encodeToBase64Str(JSON.toJSONString(o)));
        }
        modelAndView.getModelMap().putAll(result);
    }
}
