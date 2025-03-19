package org.agentpower.service.secure.recognization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVo {
    private String id;
    private String username;
    private String email;
    private String phone;
    /** privileges 的格式  p1;p2; */
    private String privileges;
}