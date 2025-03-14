package org.agentpower.service.secure.recognization;

import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public abstract class Recognizer {
    private final String headerField;
    public final String headerField() {
        return headerField;
    }
    public abstract Optional<LoginUserVo> recognize(String token);

    public abstract String generateToken(LoginUserVo user);
}
