package org.agentpower.service.secure.codec;

import cn.hutool.crypto.asymmetric.SignAlgorithm;
import lombok.extern.log4j.Log4j2;
import org.agentpower.api.CommonResponse;
import org.agentpower.common.RSAUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

@Log4j2
@Controller
@RequestMapping("/codec")
public class GenerateCodecKeyPairController {

    @GetMapping("list")
    public CommonResponse<String> list() {
        return CommonResponse.success(Stream.of(SignAlgorithm.values())
                .map(SignAlgorithm::name)
                .filter(name -> name.contains("RSA"))
                .toList());
    }

    @GetMapping("generate")
    public CommonResponse<Map<String, Object>> generate(@RequestParam(required = false) String algorithm) {
        log.debug("GenerateCodecKeyPair.generate::{}", algorithm);
        algorithm = StringUtils.isBlank(algorithm) ? "SHA512withRSA" : algorithm;
        KeyPair keyPair = RSAUtil.generateKeyPair(algorithm);
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        return CommonResponse.success(Map.of("publicKey", publicKey, "privateKey", privateKey));
    }

}
