package org.agentpower.service.secure.codec.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import org.agentpower.common.RSAUtil;
import org.agentpower.service.secure.codec.Codec;
import org.agentpower.service.secure.codec.CodecProvider;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class CodecProviderRSA extends CodecProvider {

    public CodecProviderRSA() {
        super(
                SignAlgorithm.SHA512withRSA.getValue(),
                SignAlgorithm.SHA512withRSA_PSS.getValue()
        );
    }

    @Override
    protected Codec GenerateMyCodec(String algorithm, String keyForEncode, String keyForDecode) {
        PublicKey publicKey = RSAUtil.generatePublicKey(algorithm, FileUtil.readBytes(keyForEncode));
        PrivateKey privateKey = RSAUtil.generatePrivateKey(algorithm, FileUtil.readBytes(keyForDecode));
        RSA rsa = RSAUtil.create(publicKey, privateKey);
        return new MyCodec(rsa);
    }

    private record MyCodec(RSA rsa) implements Codec {
        @Override
        public byte[] encode(byte[] data) {
            return RSAUtil.encrypt(rsa, data);
        }

        @Override
        public byte[] decode(byte[] data) {
            return RSAUtil.decryptBase64StrInUtf8Bytes(rsa, data);
        }
    }
}
