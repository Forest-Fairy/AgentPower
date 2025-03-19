package org.agentpower.service.secure.codec.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import org.agentpower.common.RSAUtil;
import org.agentpower.service.secure.codec.Decoder;
import org.agentpower.service.secure.codec.CodecProvider;
import org.agentpower.service.secure.codec.Encoder;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@Component
public class CodecProviderRSA extends CodecProvider {

    public CodecProviderRSA() {
        super(
                SignAlgorithm.SHA512withRSA.getValue(),
                SignAlgorithm.SHA512withRSA_PSS.getValue()
        );
    }

    @Override
    protected Decoder generateDecoder(String algorithm, String keyForDecode) {
        PrivateKey privateKey = RSAUtil.generatePrivateKey(algorithm,
                Base64.getDecoder().decode(keyForDecode));
        return new MyDecoder(RSAUtil.createToDecode(privateKey));
    }

    @Override
    protected Encoder generateEncoder(String algorithm, String keyForEncode) {
        PublicKey publicKey = RSAUtil.generatePublicKey(algorithm,
                Base64.getDecoder().decode(keyForEncode));
        return new MyEncoder(RSAUtil.createToEncrypt(publicKey));
    }

    private record MyDecoder(RSA rsa) implements Decoder {
        @Override
        public byte[] decode(byte[] data) {
            return RSAUtil.decryptBase64StrInUtf8Bytes(rsa, data);
        }
    }
    private record MyEncoder(RSA rsa) implements Encoder {
        @Override
        public byte[] encode(byte[] data) {
            return RSAUtil.encrypt(rsa, data);
        }
    }
}
