package org.agentpower.user.service.impl;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.crypto.digest.MD5;
import org.agentpower.api.CommonResponse;
import org.agentpower.service.secure.codec.OutputCodec;
import org.agentpower.service.secure.recognization.LoginUserVo;
import org.agentpower.service.secure.recognization.Recognizer;
import org.agentpower.user.model.UserModel;
import org.agentpower.user.model.request.UserLoginRequest;
import org.agentpower.user.model.request.UserRegisterRequest;
import org.agentpower.user.repository.UserRepo;
import org.apache.tika.parser.mailcommons.MailUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

public class UserLoginService {
    private final OutputCodec outputCodec;
    private final Recognizer recognizer;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepo repo;

    public UserLoginService(OutputCodec outputCodec, Recognizer recognizer, RedisTemplate<String, String> redisTemplate, UserRepo repo) {
        this.outputCodec = outputCodec;
        this.recognizer = recognizer;
        this.redisTemplate = redisTemplate;
        this.repo = repo;
    }

    public CommonResponse<String> loginWithPassword(UserLoginRequest loginRequest) {
        String password = MD5.create().digestHex16(loginRequest.getPassword());
        String infoToken = Optional.ofNullable(loginRequest.getInfoToken()).orElse("");
        UserModel userModel = null;
        if (isUid(infoToken)) {
            userModel = repo.findByUidAndPassword(infoToken, password);
        } else if (isChinaMobilePhone(infoToken)) {
            userModel = repo.findByPhoneAndPassword(infoToken, password);
        } else if (isEmail(infoToken)) {
            userModel = repo.findByEmailAndPassword(infoToken, password);
        } else if (isUsername(infoToken)) {
            userModel = repo.findByUsernameAndPassword(infoToken, password);
        }
        if (userModel == null) {
            return CommonResponse.failure("登录信息有误，用户名或密码错误");
        }
        return CommonResponse.success(generateUserToken(userModel));
    }

    public CommonResponse<String> loginWithCode(UserLoginRequest loginRequest) {
        String code = loginRequest.getCode().toUpperCase();
        Boolean exist = redisTemplate.delete(
                buildRedisKey(code, loginRequest.getInfoToken()));
        UserModel userModel = null;
        if (exist != null && exist) {
            userModel = repo.findByEmailOrPhone(loginRequest.getInfoToken(), loginRequest.getInfoToken());
        }
        if (userModel == null) {
            return CommonResponse.failure("登录信息有误，验证码信息有误");
        }
        return CommonResponse.success(generateUserToken(userModel));
    }

    public CommonResponse<String> registerWithEmail(UserRegisterRequest registerRequest) {
        if (isEmail(registerRequest.getEmail())) {
            Boolean exist = redisTemplate.delete(
                    buildRedisKey(registerRequest.getEmailCode(), registerRequest.getEmail()));
            if (exist != null && exist) {
                if (repo.findByEmailOrPhone(registerRequest.getEmail(), null) != null) {
                    return CommonResponse.failure("注册失败，邮箱已被注册");
                }
                UserModel userModel = new UserModel();
                userModel.setEmail(registerRequest.getEmail());
                userModel.setPassword(MD5.create().digestHex16(registerRequest.getPassword()));
                userModel.setStatus(UserModel.Status.NOT_EDIT);
                if (userModel.getId() != null) {
                    return CommonResponse.success(generateUserToken(userModel));
                }
            }
        }
        return CommonResponse.failure("注册失败，信息有误");
    }

    public CommonResponse<String> registerWithPhone(UserRegisterRequest registerRequest) {
        if (isChinaMobilePhone(registerRequest.getPhone())) {
            Boolean exist = redisTemplate.delete(
                    buildRedisKey(registerRequest.getPhoneCode(), registerRequest.getPhone()));
            if (exist != null && exist) {
                if (repo.findByEmailOrPhone(null, registerRequest.getPhone()) != null) {
                    return CommonResponse.failure("注册失败，手机号已被注册");
                }
                UserModel userModel = new UserModel();
                userModel.setPhone(registerRequest.getPhone());
                userModel.setPassword(MD5.create().digestHex16(registerRequest.getPassword()));
                userModel.setStatus(UserModel.Status.NOT_EDIT);
                userModel = repo.save(userModel);
                if (userModel.getId() != null) {
                    return CommonResponse.success(generateUserToken(userModel));
                }
            }
        }
        return CommonResponse.failure("注册失败，信息有误");
    }

    private boolean isUsername(String infoToken) {
        // 英文字母开头 长度8-12位 允许数字、字母、下划线、#、@
        return infoToken != null && infoToken.matches("^[a-zA-Z][a-zA-Z0-9_#@]{7,11}$");
    }

    private boolean isEmail(String infoToken) {
        return MailUtil.containsEmail(infoToken);
    }

    private boolean isChinaMobilePhone(String infoToken) {
        // 必须是'区号 手机号'的格式
        return infoToken != null && infoToken.contains(" ") && PhoneUtil.isMobile(infoToken);
    }

    private boolean isUid(String infoToken) {
        // 12位纯数字 是uid
        return infoToken != null && infoToken.matches("^[0-9]{12}$");
    }

    private String generateUserToken(UserModel userModel) {
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtils.copyProperties(userModel, loginUserVo);
        String userToken = recognizer.generateToken(loginUserVo);
        // 存在编码器则需要加密token
        return Optional.of(outputCodec.getEncoder())
                .map(encoder -> encoder.encodeToBase64Str(userToken))
                .orElse(userToken);
    }

    private String buildRedisKey(String code, String token) {
        return code + "#" + token;
    }
}
